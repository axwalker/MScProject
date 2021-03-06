/*jshint strict: false */

function initCy() {
    var maxSize = viewModel.metadata().maxCommunitySize;
    var maxEdge = viewModel.metadata().maxEdgeConnection;

    var nodeCss;
    var edgeCss;
    var nodeCount = viewModel.graph().nodes.length;

    var minNodeSize = Math.max(3, 500 / nodeCount);
    minNodeSize = Math.min(minNodeSize, 10);
    var maxNodeSize = Math.min(50, 5000 / nodeCount);
    maxNodeSize = Math.max(maxNodeSize, 50);

    viewModel.cancelLayoutStatus(false);

    if (viewModel.currentLevel() === 0 || (viewModel.currentLevel() === 1 && $( "#algorithm option:selected" ).text() === 'ORCA')) {
        nodeCss = {
            'background-color': 'data(colour)',
            'height': Math.min(900 / nodeCount, 30),
            'width': Math.min(900 / nodeCount, 30),
            'font-size': Math.min(550 / nodeCount, 20)
        };
        edgeCss = {
            'line-color': '#53433F',
            'width': Math.min(18 / nodeCount, 2)
        };
    } else {
        nodeCss = {
            'background-color': 'data(colour)',
            'height': 'mapData(size, 1, ' + maxSize + ', ' + minNodeSize + ', ' + maxNodeSize + ')',
            'width': 'mapData(size, 1, ' + maxSize + ', ' + minNodeSize + ', ' + maxNodeSize + ')',
            'font-size': 'mapData(size, 1, ' + maxSize + ', ' + minNodeSize + ', ' + maxNodeSize + ')'
        };
        edgeCss = {
            'line-color': '#53433F',
            'width': 'mapData(weight, 0, ' + maxEdge + ', 0.2, 5)',
            //'curve-style': 'haystack'
        };
    }

    var styleOptions = [
        {
            selector: 'node',
            css: nodeCss
        },
        {
            selector: 'node:selected',
            css: {
                'border-color': 'black',
                'border-width': '4',
                'line-color': '#000'                                
            }
        },
        {
            selector: 'edge',
            css: edgeCss
        }
    ];

    $('#cy').cytoscape({

        style: styleOptions,
    
        layout: arborLayout(),

        elements: viewModel.graph(),

        hideEdgesOnViewport: nodeCount > 250,
        hideLabelsOnViewport: nodeCount > 250,
        textureOnViewport: nodeCount > 250,

        ready: function(){
            window.cy = this;

            cy.boxSelectionEnabled(false);

            cy.on('click', 'node', function(evt){
                if (!viewModel.isBottomLevel() && !(viewModel.currentLevel() === 1 && $( "#algorithm option:selected" ).text() === 'ORCA')) {
                    this.select();
                    viewModel.selectedCommunity(this.data('id'));
                }
            });

            if (viewModel.currentLevel() === 0) {
                cy.nodes().qtip({
    				content: {
                        text: function(){ 
                            var dataText = '';
                            var metadata = this.data('metadata');
                            for (var key in metadata) {
                                if (key !== 'id') {
                                    dataText += '<b>' + key + ':</b> ' + metadata[key] + '<br>';
                                }
                            }
                            return dataText;
                        }
                    },
    				position: {
    					my: 'top center',
    					at: 'bottom center'
    				},
    				style: {
    					classes: 'qtip-bootstrap',
    					tip: {
    						width: 16,
    						height: 8
    					}
    				},
                    show: {
                        event: 'mouseover'
                    },
                    hide: {
                        event: 'mouseout'
                    },
    			});
            }
            
            cy.edges().qtip({
				content: function(){ 
                    return 'Weight: ' + parseFloat(this.data('weight').toFixed(3));
                },
				position: {
					my: 'top center',
					at: 'bottom center'
				},
				style: {
					classes: 'qtip-bootstrap',
					tip: {
						width: 16,
						height: 8
					}
				},
                show: {
                    event: 'click'
                },
                hide: {
                    delay: 400,
                    fixed: true,
                    effect: function() { $(this).fadeOut(250); }
                },
			});
        }

    });
    viewModel.cy($('#cy').cytoscape('get'));
}

function clearCy() {
    $('#cy').cytoscape({});
}

function arborLayout() {
    return {
        name: 'arbor',

        liveUpdate: true,
        maxSimulationTime: viewModel.layoutTime() * 1000,
        fit: true, // reset viewport to fit default simulationBounds
        padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
        ungrabifyWhileSimulating: true,
        stepSize: 1,

        repulsion: viewModel.layoutRepulsion(),
        stiffness: viewModel.layoutStiffness(),
        friction: viewModel.layoutFriction(),
        gravity: true,
        fps: undefined,
        precision: undefined,

        /*nodeMass: function() {
            var maxSize = viewModel.metadata().maxCommunitySize;
            //return this.data('size') / maxSize;
            if (this.data('size') < maxSize/2) {
                return 1;
            } else {
                return 10;
            }
        },
        edgeLength: function() {
            return 1 / this.data('weight');
        },*/

        stableEnergy: function( energy ){
          var e = energy; 
          return (e.max <= 0.5) || (e.mean <= 0.3) || viewModel.cancelLayoutStatus();
        },

        ready: function() {
            viewModel.isArborRunning(true);
            $('#refreshButton').attr('disabled', true);
            //alertify.success('Laying out graph');
        },
        
        stop: function() {
            viewModel.isArborRunning(false);
            $('#refreshButton').attr('disabled', false);
        }
    };
}