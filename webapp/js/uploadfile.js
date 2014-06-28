$('#uploadButton').click(function(){
    var formData = new FormData($('form')[0]);
    console.log("running upload");
    $.ajax({
        type: 'POST',
        url: 'RunLabelPropagation',
        data: formData,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                console.log("java says success!");
                cy = cytoscape({
                    container: document.getElementById('cy'),
                    style: 'node { background-color: green; content : data(label); }',
                    layout: {
                        name: 'arbor',

                            liveUpdate: true, // whether to show the layout as it's running
                            ready: undefined, // callback on layoutready 
                            stop: undefined, // callback on layoutstop
                            maxSimulationTime: 10000, // max length in ms to run the layout
                            fit: true, // reset viewport to fit default simulationBounds
                            padding: [ 50, 50, 50, 50 ], // top, right, bottom, left
                            simulationBounds: undefined, // [x1, y1, x2, y2]; [0, 0, width, height] by default
                            ungrabifyWhileSimulating: true, // so you can't drag nodes during layout

                            // forces used by arbor (use arbor default on undefined)
                            repulsion: undefined,
                            stiffness: undefined,
                            friction: undefined,
                            gravity: true,
                            fps: undefined,
                            precision: undefined,

                            // static numbers or functions that dynamically return what these
                            // values should be for each element
                            nodeMass: undefined, 
                            edgeLength: undefined,

                            stepSize: 1, // size of timestep in simulation

                            // function that returns true if the system is stable to indicate
                            // that the layout can be stopped
                            stableEnergy: function( energy ){
                              var e = energy; 
                              return (e.max <= 0.5) || (e.mean <= 0.3);
                            }
                    },
                });
                cy.load(data, 
                function(e){
                    $("#ajaxResponse").html("<div>Laying out elements...</div>");
                }, 
                function(e){
                    $("#ajaxResponse").html("<div>Graph laid out.</div>");
                });
            } else {
                console.log("java says failure");
                console.log(data.error);
                $("#ajaxResponse").html("<div><b>Failed to process graph</b></div>");
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#myButton').attr("disabled", true);
        },

        complete: function(jqXHR, textStatus){
            $('#myButton').attr("disabled", false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log("Something really bad happened " + textStatus);
            $("#ajaxResponse").html("<div><b>Failed to process graph: POST error</b></div>");
        },
        
        cache: false,
        contentType: false,
        processData: false
    });
});
