$('#uploadButton').click(function(){
    var formData = new FormData($('form')[0]);
    var arborSeconds = document.getElementById('arborTime').value * 1000;
    $.ajax({
        type: 'POST',
        url: 'RunLabelPropagation',
        data: formData,
        dataType: "json",

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                $("#ajaxResponse").append("<li>>: Graph processed.</li>");
                console.log('#' + intToARGB(hashCode('n2')));
                cy = cytoscape({
                    container: document.getElementById('cy'),
                    
                    style: [
                        { 
                            selector: 'node',
                            css: {
                                'content': 'data(id)',
                                'background-color': 'data(colour)',
                                'text-valign': 'top',
                                'text-halign': 'center'
                            }
                        },
                        {
                            selector: 'edge',
                            css: {
                                //'line-color': 'black'
                            }
                        }
                    ],

                    layout: {
                        name: 'arbor',

                            liveUpdate: true, // whether to show the layout as it's running
                            ready: undefined, // callback on layoutready 
                            stop: undefined, // callback on layoutstop
                            maxSimulationTime: arborSeconds, // max length in ms to run the layout
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
                    $("#ajaxResponse").append("<li>>: Laying out elements...</li>");
                    $(".footer").scrollTop($(".footer")[0].scrollHeight);
                }, 
                function(e){
                    $("#ajaxResponse").append("<li>>: Graph laid out.</li>");
                    $(".footer").scrollTop($(".footer")[0].scrollHeight);
                });
            } else {
                console.log(data.error);
                $("#ajaxResponse").append("<li><b>>: Failed to process graph</b></li>");
                $(".footer").scrollTop($(".footer")[0].scrollHeight);
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#uploadButton').attr("disabled", true);
            $("#ajaxResponse").append("<li>>: Uploading graph...</li>");
            $(".footer").scrollTop($(".footer")[0].scrollHeight);
        },

        complete: function(jqXHR, textStatus){
            $('#uploadButton').attr("disabled", false);
            $(".footer").scrollTop($(".footer")[0].scrollHeight);
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

function hashCode(str) { // java String#hashCode
    var hash = 0;
    for (var i = 0; i < str.length; i++) {
       hash = str.charCodeAt(i) + ((hash << 5) - hash);
    }
    return hash;
} 

function intToARGB(i){
    return ((i>>24)&0xFF).toString(16) + 
           ((i>>16)&0xFF).toString(16) + 
           ((i>>8)&0xFF).toString(16) + 
           (i&0xFF).toString(16);
}
