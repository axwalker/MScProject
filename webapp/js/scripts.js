/*jshint strict: false */

function GraphView(drillLevel, colourLevel, currentLevel, community) {
    this.drillLevel = drillLevel;
    this.colourLevel = colourLevel;
    this.currentLevel = currentLevel;
    this.community = community;
}

var graphRequest = function(url, formData, processData, contentType, dataFunction) {
    $.ajax({
        type: 'POST',
        url: url,
        data: formData,
        processData: processData,
        contentType: contentType,
        dataType: 'json',
        cache: false,

        success: function( data, textStatus, jqXHR) {
            if(data.success) {
                dataFunction(data);
            } else {
                alertify.alert('There was a problem: <br><br>' + data.error);
                clearCy();
                viewModel.graph(false);
                viewModel.cancelLayoutStatus(true);
            }
        },

        beforeSend: function(jqXHR, settings) {
            viewModel.loadingUpdate(true);
            $('#uploadButton').attr('disabled', true);
        },

        complete: function(jqXHR, textStatus){
            clearInterval(viewModel.intervalId);
            if (viewModel.graph()) {
                progress(100, $('#progressBar'), 1000, function() {
                    viewModel.loadingGraph(false);
                });
            } else {
                viewModel.loadingGraph(false);
            }
            $('#uploadButton').attr('disabled', false);
            viewModel.loadingUpdate(false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log('Something really bad happened ' + textStatus);
            clearCy();
            viewModel.loadingGraph(false);
            viewModel.loadingUpdate(false);
            viewModel.graph(false);
            viewModel.cancelLayoutStatus(true);
            alertify.alert('There was a problem with the server, please refresh and try again');
        }
    });
};

var initialiseGraph = function(data) {
    var algorithm = $('#algorithm').val();
    var filename = $('#fileInput').val().split(/\\/).pop();
    document.title = algorithm + ' - ' + filename;
    
    viewModel.graph(data);
    if (data.nodes.length < MAX_NODES_VIEWABLE) {
        initCy(viewModel.graph());
    } else {
        alertify.alert('There are too many communities to display.' +
            ' GML files of this community structure are available for download.');
        viewModel.currentViewTooBig(true);
    }
};

var refreshColours = function(data) {
    var nodes = data.nodes;
    nodes.forEach( function (node) {
        var id = node.data.id;
        var newColour = node.data.colour;
        var newMetadata = node.data.metadata;
        var cyNode = viewModel.cy().elements('node#' + id)[0];
        cyNode.data('colour', newColour);
        cyNode.data('metadata', newMetadata);
        cyNode.css('background-color', newColour);
    });
    alertify.success('Updated colours');
};

function progress(percent, $element, speed, callback) {
    callback = callback || $.noop;
    var progressBarWidth = percent * $element.width() / 100;
    $element.find('div').stop(true, true).animate({
            width: progressBarWidth
        },
        {
            queue: false,
            duration: speed,
            complete: function () {
                setTimeout( callback, 500); 
            }  
        }
    );
}

$(document).ready(function() {
   $('#layoutSettings').qtip({
       content: {
        text: $('#layoutSettings').next('.tooltiptext')
        },
        position: {
            my: 'bottom center',
            at: 'top center'
        },
        style: {
            classes: 'qtip-bootstrap'
        },
        hide: {
            delay: 400,
            fixed: true,
            effect: function() { $(this).fadeOut(250); }
        },
        show: {
            solo: true
        }
    });

   $('span.help').each(function() {
        $(this).qtip({
            content: {
                text: $(this).next('.helpContent')
            },
            position: {
                my: $(this).hasClass('topHelp') ? 'top center' : 'bottom center',
                at: $(this).hasClass('topHelp') ? 'bottom center' : 'top center'
            },
            style: {
                classes: 'qtip-bootstrap'
            },
            hide: {
                delay: 400,
                fixed: true,
                effect: function() { $(this).fadeOut(250); }
            },
            show: {
                solo: true
            }
        });
    });
});