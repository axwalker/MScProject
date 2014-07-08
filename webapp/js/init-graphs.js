$("#cy.high, #cy.low").hide();
$("#cy.high").show();

function initHigh(data) {
    var metadata = graphs.compoundGraph.HighLevel.metadata;
    var minSize = metadata.minCommunitySize;
    var maxSize = metadata.maxCommunitySize;
    var maxEdge = metadata.maxEdgeConnection;
    var styleOptions = [
            {
                selector: 'node',
                css: {
                    'background-color': 'mapData(clusterRating, 0, 1, darkgrey, green)',
                    'height': 'mapData(size, ' + minSize + ', ' + maxSize + ', 2, 30)',
                    'width': 'mapData(size, ' + minSize + ', ' + maxSize + ', 2, 30)'
                }
            },
            {
                selector: ':selected',
                css: {
                    'border-color': '#fff',
                    'line-color': '#000'                                
                }
            },
            {
                selector: 'edge',
                css: {
                    'line-color': '#53433F',
                    'width': 'mapData(weight, 0, ' + maxEdge + ', 0.2, 5)'
                }
            }
    ];

    $('#cy.high').cytoscape({

        style: styleOptions,
    
        layout: getHighLayout(),

        elements: data,

        ready: function(){
            window.cy = this;

            cy.on('click', 'node', function(){
                $("#cy.high").hide();
                $("#cy.low").show();
                label = this.data('id');
                initLow(graphs.subGraphs[label]);
            });
            
            cy.nodes().qtip({
				content: {
                    text: function(){ 
                        return '<b>Size:</b> ' + this.data('size') + '<br>' + 
                            '<b>Intra-cluster density:</b> ' + this.data('intraClusterDensity').toFixed(2) + '<br>' +
                            '<b>Inter-cluster density:</b> ' + this.data('interClusterDensity').toFixed(2);
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
    });
    cyHigh = $('#cy.high').cytoscape('get');
}

function initLow(data) {
    var maxTime = document.getElementById('arborTimeLow').value * 1000;
    $('#cy.low').cytoscape({

        style: [
            {
                selector: 'node',
                css: {
                    'content': 'data(id)',
                    'background-color': 'data(colour)',
                    'text-valign': 'center',
                    'text-halign': 'center' 
                }
            },
            {
                selector: ':selected',
                css: {
                    'border-color': '#fff',
                    'line-color': '#000'                                
                }
            }, 
            {
                selector: 'edge',
                css: {
                    'line-color': '#53433F'
                }
            }
        ],

        layout: getHighLayout(maxTime),
        
        elements: data,

        ready: function(){
            window.cy = this;

            cy.on('click', 'node', function(){
                $("#cy.low").hide();
                $("#cy.high").show();
            });
        }
    });
    cyLow = $('#cy.low').cytoscape('get');
}

function getHighLayout() {
    var maxTime = document.getElementById('arborTimeHigh').value * 1000;
    return {
        name: 'arbor',

            liveUpdate: true,
            maxSimulationTime: maxTime,
            fit: true, // reset viewport to fit default simulationBounds
            padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
            ungrabifyWhileSimulating: true,

            edgeLength: function( edge ) {
                return 0.01/(edge.weight);
            },

            stepSize: 0.1,

            stableEnergy: function( energy ){
              var e = energy; 
              return (e.max <= 0.5) || (e.mean <= 0.3);
            },

            ready: function() {
                $("#ajaxResponse").append("<li>>: Laying out elements...</li>");
            },
            stop: function() {
                $("#ajaxResponse").append("<li>>: Layout complete.</li>");
            }
    };
}

function getLowLayout(maxTime) {
    return {
        name: 'arbor',

            liveUpdate: true, // whether to show the layout as it's running
            maxSimulationTime: 10000, //arborSeconds, // max length in ms to run the layout
            fit: true, // reset viewport to fit default simulationBounds
            padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
            ungrabifyWhileSimulating: true, // so you can't drag nodes during layout

            stepSize: 0.1,

            stableEnergy: function( energy ){
              var e = energy; 
              return (e.max <= 0.5) || (e.mean <= 0.3);
            }
    };
}