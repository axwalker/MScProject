$("#cy.high, #cy.low").hide();
$("#cy.high").show();

$('#cy.high').cytoscape({

    style: [
        {
            selector: 'node',
            css: {
                'background-color': 'data(colour)',
                'height': 'mapData(size, 5, 100, ' + '1' + ', 20)',
                'width': 'mapData(size, 5, 100, 1, 20)'
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
                'width': 'mapData(weight, 0, 20, 0, 5)'
            }
        }
    ],

    layout: {
        name: 'arbor',

            liveUpdate: true, // whether to show the layout as it's running
            ready: undefined, // callback on layoutready 
            stop: undefined, // callback on layoutstop
            maxSimulationTime: 20000, //arborSeconds, // max length in ms to run the layout
            fit: true, // reset viewport to fit default simulationBounds
            padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
            ungrabifyWhileSimulating: true, // so you can't drag nodes during layout

            edgeLength: function( edge ) {
                return 0.01/(edge.weight);
            },

            stepSize: 0.1,

            stableEnergy: function( energy ){
              var e = energy; 
              return (e.max <= 0.5) || (e.mean <= 0.3);
            }
    },
    ready: function(){
        window.cy = this;
        console.log('ready');
        //$('#cy').cytoscapeNavigator()

        cy.on('click', 'node', function(){
            $("#cy.high").hide();
            $("#cy.low").show();
            label = this.data('id');
            cyLow.load(graphs[label]);
        });
    }
});

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
        }
    ],

    layout: {
        name: 'arbor',

            liveUpdate: true, // whether to show the layout as it's running
            ready: undefined, // callback on layoutready 
            stop: undefined, // callback on layoutstop
            maxSimulationTime: 5000, //arborSeconds, // max length in ms to run the layout
            fit: true, // reset viewport to fit default simulationBounds
            padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
            ungrabifyWhileSimulating: true, // so you can't drag nodes during layout

            stepSize: 0.1,

            stableEnergy: function( energy ){
              var e = energy; 
              return (e.max <= 0.5) || (e.mean <= 0.3);
            }
    },
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

var cyHigh = $('#cy.high').cytoscape('get');
var cyLow = $('#cy.low').cytoscape('get');

$('#uploadButton').click(function(){
    var formData = new FormData($('form')[0]);
    //var arborSeconds = document.getElementById('arborTime').value * 1000;
    $.ajax({
        type: 'POST',
        url: 'ProcessGraph',
        data: formData,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                $("#ajaxResponse").append("<li>>: Graph processed.</li>");
                graphs = data.graphs;
                console.log("in success: " + JSON.stringify(graphs, undefined, 2));
                
                cyHigh.load(graphs.HighLevel, 
                    function(e){
                        $("#ajaxResponse").append("<li>>: Laying out elements...</li>");
                        //$(".footer").scrollTop($(".footer")[0].scrollHeight);
                    }, 
                    function(e){
                        $("#ajaxResponse").append("<li>>: Graph laid out.</li>");
                        //$('#cy').cytoscapeNavigator('resize');
                    });
            } else {
                console.log(data.error);
                $("#ajaxResponse").append("<li><b>>: Failed to process graph</b></li>");
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#uploadButton').attr("disabled", true);
            $("#ajaxResponse").append("<li>>: Uploading graph...</li>");
        },

        complete: function(jqXHR, textStatus){
            $('#uploadButton').attr("disabled", false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
            $("#ajaxResponse").append("<li><b>>: Failed to process graph: POST error</b></li>");
        },
        
        cache: false,
        contentType: false,
        processData: false
    });
});
