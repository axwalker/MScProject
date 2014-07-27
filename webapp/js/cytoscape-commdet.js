/*jshint strict: false */

function initCy() {
    var maxSize = viewModel.maxCommunitySize();
    var maxEdge = viewModel.maxEdgeConnection();

    var nodeCss;
    var edgeCss;
    var nodeCount = viewModel.graph().nodes.length;

    if (viewModel.currentLevel() !== 0) {
        nodeCss = {
            'background-color': 'data(colour)',
            'height': 'mapData(size, 1, ' + maxSize + ', 2, 30)',
            'width': 'mapData(size, 1, ' + maxSize + ', 2, 30)'
        };
        edgeCss = {
            'line-color': '#53433F',
            'width': 'mapData(weight, 1, ' + maxEdge + ', 0.2, 5)'
        };
    } else {
        nodeCss = {
            'background-color': 'data(colour)',
            'height': Math.min(750 / nodeCount, 30),
            'width': Math.min(750 / nodeCount, 30),
            'font-size': Math.min(750 / nodeCount, 20)
        };
        edgeCss = {
            'line-color': '#53433F',
            'width': Math.min(18 / nodeCount, 2)
        };
    }

    var styleOptions = [
            {
                selector: 'node',
                css: nodeCss
            },
            {
                selector: ':selected',
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
    
        layout: viewModel.layoutChoiceComputed(),

        elements: viewModel.graph(),

        ready: function(){
            window.cy = this;

            $('#cy').cytoscapeNavigator('destroy');
            $('#cy').cytoscapeNavigator();

            cy.on('click', 'node', function(evt){
                this.select();
                viewModel.selectedCommunity(this.data('id'));
            });
            
            cy.nodes().qtip({
				content: {
                    text: function(){ 
                        var dataText = '';
                        if (viewModel.currentLevel() === 0 && this.data('metadata')) {
                            var metadata = this.data('metadata');
                            for (var key in metadata) {
                                dataText += '<b>' + key + ':</b> ' + metadata[key] + '<br>';
                            }
                        } else {
                            dataText = '<b>Size:</b> ' + this.data('size') + '<br>';
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
                }
			});
            
            cy.edges().qtip({
				content: function(){ 
                    return 'Connections: ' + this.data('weight');
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
                }
			});

        }
    }).cytoscape(function(){
        //$('#cy').cytoscapeNavigator();
        //$('#cy').trigger('resize');
        //$('#cy').cytoscapeNavigator('resize')
    });
    viewModel.cy($('#cy').cytoscape('get'));
}

function arborLayout(maxTime) {
    return {
        name: 'arbor',
        liveUpdate: true,
        maxSimulationTime: maxTime * 1000,
        fit: true, // reset viewport to fit default simulationBounds
        padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
        ungrabifyWhileSimulating: true,
        stepSize: 0.1,

        edgeLength: function( edge ) {
            return 0.01/(edge.weight);
        },

        stableEnergy: function( energy ){
          var e = energy; 
          return (e.max <= 0.5) || (e.mean <= 0.3) || viewModel.cancelLayoutStatus();
        },

        ready: function() {
            viewModel.isArborRunning(true);
            $('#refreshButton').attr("disabled", true);
            viewModel.status('Layout complete');
        },
        
        stop: function() {
            viewModel.isArborRunning(false);
            $('#refreshButton').attr("disabled", false);
            viewModel.status('Layout complete');

            //hack to fix graph not refreshing if time remains unchanged:
            var previousTime = viewModel.layoutTime();
            viewModel.layoutTime(-1);
            viewModel.layoutTime(previousTime);
        }
    };
}

function defaultArborLayout(maxTime) {
    return {
        name: 'arbor',

        liveUpdate: true, // whether to show the layout as it's running
        maxSimulationTime: maxTime * 1000, // max length in ms to run the layout
        fit: true, // reset viewport to fit default simulationBounds
        padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
        simulationBounds: undefined, // [x1, y1, x2, y2]; [0, 0, width, height] by default
        ungrabifyWhileSimulating: true, // so you can't drag nodes during layout
        gravity: true,
        stepSize: 1, // size of timestep in simulation

        stableEnergy: function( energy ){
          var e = energy; 
          return (e.max <= 0.5) || (e.mean <= 0.3) || viewModel.cancelLayoutStatus();
        },

        ready: function() {
            viewModel.isArborRunning(true);
            $('#refreshButton').attr("disabled", true);
            viewModel.status('Layout complete');
        },
        
        stop: function() {
            viewModel.isArborRunning(false);
            $('#refreshButton').attr("disabled", false);
            viewModel.status('Layout complete');

            //hack to fix graph not refreshing if time remains unchanged:
            var previousTime = viewModel.layoutTime();
            viewModel.layoutTime(-1);
            viewModel.layoutTime(previousTime);
        }
    }
}

function gridLayout() {
    return {
        name: 'grid',

        fit: true, // whether to fit the viewport to the graph
        padding: 30, // padding used on fit
        rows: undefined, // force num of rows in the grid
        columns: undefined, // force num of cols in the grid
        position: function( node ){}, // returns { row, col } for element
        ready: undefined, // callback on layoutready
        stop: undefined // callback on layoutstop
    };
}

function circleLayout() {
    return {
        name: 'circle',

        fit: true, // whether to fit the viewport to the graph
        ready: undefined, // callback on layoutready
        stop: undefined, // callback on layoutstop
        rStepSize: 10, // the step size for increasing the radius if the nodes don't fit on screen
        padding: 30, // the padding on fit
        startAngle: 3/2 * Math.PI, // the position of the first node
        counterclockwise: false // whether the layout should go counterclockwise (true) or clockwise (false)
    };
}

function breadthfirstLayout() {
    return {
        name: 'breadthfirst',

        fit: true, // whether to fit the viewport to the graph
        ready: undefined, // callback on layoutready
        stop: undefined, // callback on layoutstop
        directed: false, // whether the tree is directed downwards (or edges can point in any direction if false)
        padding: 30, // padding on fit
        circle: false, // put depths in concentric circles if true, put depths top down if false
        roots: undefined, // the roots of the trees
        maximalAdjustments: 0 // how many times to try to position the nodes in a maximal way (i.e. no backtracking)
    };
}