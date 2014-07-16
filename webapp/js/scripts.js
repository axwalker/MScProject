/*jshint strict: false */

var viewModel = function() {
    var self = this;

    self.status = ko.observable();
    self.graph = ko.observable();
    self.cy = ko.observable();

    // RESULTS -----
    self.metadata = ko.computed( function() {
        return self.graph() ? self.graph().metadata : '';
    });

    self.modularity = ko.computed(function() {
        return self.graph() ? self.metadata().modularity.toFixed(2) : '';
    });

    self.minCommunitySize = ko.computed(function() {
        return self.metadata().minCommunitySize;
    });

    self.maxCommunitySize = ko.computed(function() {
        return self.metadata().maxCommunitySize;
    });

    self.avgCommunitySize = ko.computed(function() {
        return self.metadata().avgCommunitySize;
    });

    self.maxEdgeConnection = ko.computed(function() {
        return self.metadata().maxEdgeConnection;
    });

    self.hierarchyHeight = ko.computed(function() {
        return self.metadata().hierarchyHeight;
    });

    self.currentLevel = ko.computed(function() {
        return self.metadata().currentLevel;
    });

    self.isBottomLevel = ko.computed(function() {
        return self.currentLevel() === 0;
    });

    self.onlySelectedCommunity = ko.observable(false);

    self.selectedCommunity = ko.observable();

    self.availableLevels = ko.computed(function() {
        var levels = [];
        if (self.onlySelectedCommunity()) {
            for (var i = 0; i < self.currentLevel(); i++) {
                levels.push(i);
            }
        } else {
            for (var j = 0; j <= self.hierarchyHeight(); j++) {
                levels.push(j);
            }
        }
        return levels;
    });

    // NODE LABELS -----
    self.hasLabels = ko.observable(false);

    self.labelButton = ko.computed(function() {
        return self.hasLabels() ? 'Hide node labels' : 'Show node labels';
    });

    self.toggleLabels = function() {
        if (self.hasLabels()) {
            self.cy().style()
                .selector('node')
                .css('content', '')
                .update();
            self.hasLabels(false);
        } else {
            self.cy().style()
                .selector('node')
                .css('content', 'data(id)')
                .update();
            self.hasLabels(true);
        }
    };

    // DISPLAY OPTIONS -----
    self.layoutOptions = ['Force-directed', 'Grid', 'Breadth-first', 'Circle'];

    self.layoutChoice = ko.observable('Force-directed');

    self.layoutTime = ko.observable(10);

    self.isArborRunning = ko.observable(false);

    self.cancelLayoutStatus = ko.observable(false);

    self.layoutChoiceComputed = ko.computed(function() {
        if (self.layoutChoice() === 'Force-directed') {
            self.cancelLayoutStatus(false);
            return arborLayout(self.layoutTime());
        } else if (self.layoutChoice() === 'Grid') {
            return gridLayout();
        } else if (self.layoutChoice() === 'Breadth-first') {
            return breadthfirstLayout();
        } else {
            return circleLayout();
        }
    });

    self.usesArborLayout = ko.computed(function() {
        return self.layoutChoice() === 'Force-directed';
    });

    self.cancelLayout = function() {
        self.cancelLayoutStatus(true);
    };

    self.refreshLayout = function() {
        self.cy().layout(self.layoutChoiceComputed());
    };

    // UPLOAD -----
    self.fileValue = ko.observable();

    self.hasAddedFile = ko.observable(false);

    self.addedFile = function() {
        self.fileValue($('input[type=file]').val());
        if (self.fileValue()) {
            self.hasAddedFile(true);
        } else {
            self.hasAddedFile(false);
        }
    };

    self.uploadGraph = function() {
        var formData = new FormData($('form')[0]);
        graphRequest('ProcessGraph', formData, false, false);
    };

    self.updateGraph = function() {
        var formData = 'graphLevel=' + encodeURIComponent($('#drillLevel').val());
        if (self.onlySelectedCommunity()) {
            formData += '&currentLevel=' + encodeURIComponent(self.currentLevel());
            formData += '&selectedNode=' + encodeURIComponent(self.selectedCommunity());
            self.onlySelectedCommunity(false);
        }
        graphRequest('UpdateGraph', formData, true, 'application/x-www-form-urlencoded');
    };
};

viewModel = new viewModel();

ko.applyBindings(viewModel);

var graphRequest = function(url, formData, processData, contentType) {
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
                viewModel.status('Graph processed');
                viewModel.graph(data);
                //console.log("in success: " + JSON.stringify(data, undefined, 2));
                initCy(viewModel.graph());
            } else {
                console.log(data.error);
                viewModel.status('Exception: failed to process graph');
            }
        },

        beforeSend: function(jqXHR, settings) {
            $('#uploadButton').attr('disabled', true);
            viewModel.status('Processing graph...');
        },

        complete: function(jqXHR, textStatus){
            $('#uploadButton').attr('disabled', false);
        },

        error: function(jqXHR, textStatus, errorThrown){
            console.log('Something really bad happened ' + textStatus);
            viewModel.status('Failed to process graph: POST error');
        }
    });
};