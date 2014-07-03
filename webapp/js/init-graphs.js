$("#cy.high, #cy.low").hide();
$("#cy.high").show();

function initHigh(data, minSize, maxSize) {
    var maxTime = document.getElementById('arborTimeHigh').value * 1000;
    var styleOptions = [
            {
                selector: 'node',
                css: {
                    'background-color': 'blue',
                    'height': 'mapData(size, ' + minSize + ', ' + maxSize + ', 1, 20)',
                    'width': 'mapData(size, ' + minSize + ', ' + maxSize + ', 1, 20)'
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
                    'width': 'mapData(weight, 0, 20, 0, 5)'
                }
            }
    ];

    $('#cy.high').cytoscape({

        style: styleOptions,
    
        layout: getHighLayout(maxTime),

        elements: data,

        ready: function(){
            window.cy = this;
            console.log('ready');
            //$('#cy').cytoscapeNavigator()

            cy.on('click', 'node', function(){
                $("#cy.high").hide();
                $("#cy.low").show();
                label = this.data('id');
                initLow(graphs[label]);
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
            console.log('ready');
            //$('#cy').cytoscapeNavigator()

            cy.on('click', 'node', function(){
                $("#cy.low").hide();
                $("#cy.high").show();
            });
        }
    });
    cyLow = $('#cy.low').cytoscape('get');
}

function getHighLayout(maxTime) {
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
