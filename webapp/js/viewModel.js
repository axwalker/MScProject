/*jshint strict: false */

var PAGE_SIZE = 10;
var MAX_NODES_VIEWABLE = 1000;
var MAX_EDGES_VIEWABLE = 300;
var MAX_FILESIZE = 50 * 1000 * 1000;
var MAX_FILESIZE_ORCA = 10 * 1000 * 1000;
var FILESIZE_NEEDING_PROGRESSBAR = 100* 1000;

alertify.set({ delay: 2000 });

var viewModel = function() {
    var self = this;

    self.graph = ko.observable();
    self.cy = ko.observable();
    self.loadingGraph = ko.observable(false);
    self.currentViewTooBig = ko.observable(false);
    self.loadingUpdate = ko.observable(false);

    // HISTORY ----------------------------------------------------------------------- //
    self.viewHistory = ko.observableArray();

    self.hasHistory = ko.computed( function() {
        return self.viewHistory().length > 0;
    });

    self.isShowingCommunity = ko.observable(false);

    self.currentCommunityShown = ko.computed( function() {
        if (self.viewHistory().length > 0) {
            return self.viewHistory()[self.viewHistory().length - 1].community;
        }
    });

    self.currentColourLevel = ko.computed( function() {
        if (self.viewHistory().length > 0) {
            return self.viewHistory()[self.viewHistory().length - 1].colourLevel;
        }
    });

    self.previousLevel = ko.computed( function() {
        if (self.viewHistory().length > 0) {
            return self.viewHistory()[self.viewHistory().length - 1].currentLevel;
        }
    });

    self.backHistory = function() {
        var currentView = self.viewHistory.pop();
        var previousView;
        if (self.viewHistory().length > 0) {
            previousView = self.viewHistory.pop();
            self.viewHistory.push(previousView);
        } else {
            previousView = new GraphView(self.hierarchyHeight(), self.hierarchyHeight(), self.hierarchyHeight(), -1);
        }
        if (previousView.community === -1) {
            self.updateGraphWithAjax(previousView);
        } else {
            self.updateCommunityGraphWithAjax(previousView);
        }
    };

    // RESULTS ----------------------------------------------------------------------- //
    self.allModularities = ko.computed( function() {
        return self.graph() ? self.graph().modularities : '';
    });

    self.metadata = ko.computed( function() {
        return self.graph() ? self.graph().metadata : '';
    });

    self.hierarchyHeight = ko.computed(function() {
        return self.metadata().hierarchyHeight;
    });

    self.currentLevel = ko.computed(function() {
        return self.metadata().currentLevel;
    });

    self.selectedCommunity = ko.observable();

    self.selectedCommunitySize = ko.computed( function() {
        if (self.selectedCommunity() && self.selectedCommunity() !== -1) {
            return self.cy().elements('node#' + self.selectedCommunity()).data('size');
        }
    });

    self.hasSelectedCommunity = ko.computed(function() {
        return self.selectedCommunity() && self.selectedCommunity() !== -1 && !self.isBottomLevel();
    });

    self.updateSelectedCommunity = function() {
        if (self.cy()) {
            if (self.cy().$('node:selected').length === 1) {
                self.selectedCommunity(cy.$('node:selected')[0].id());
            } else {
                self.selectedCommunity(-1);
            }
        }
    };

    self.drillLevel = ko.observable();

    self.isBottomLevel = ko.computed(function() {
        return self.drillLevel() === 0;
    });

    self.colourLevel = ko.observable();

    self.modularity = ko.computed(function() {
        var modularity = (self.colourLevel() === 0) ? 0 : self.allModularities()[self.colourLevel() - 1];
        if (modularity || modularity === 0) {
            return modularity.toFixed(2);
        }
    });

    self.levelsArray = ko.computed( function() {
        var levels = [];
        for (var i = 0; i <= self.hierarchyHeight(); i++) {
                levels.push(i);
        }
        return levels;
    });

    self.availableLevels = ko.computed(function() {
        self.drillLevel(self.currentLevel());
        return self.levelsArray().slice(0, self.hierarchyHeight() + 1);
    });

    self.colourLevels = ko.computed(function() {
        var colourLevel = (self.currentColourLevel() ? self.currentColourLevel() : self.drillLevel());
        self.colourLevel(colourLevel);
        var from = Math.max(self.drillLevel(), 1);
        return self.levelsArray().slice(from, self.hierarchyHeight() + 1);
    });

    self.communityDrillLevel = ko.observable();

    self.communityColourLevel = ko.observable();

    self.communityAvailableLevels = ko.computed(function() {
        self.communityDrillLevel(self.currentLevel() - 1);
        return self.levelsArray().slice(0, self.currentLevel());
    });

    self.communityColourLevels = ko.computed(function() {
        self.colourLevel(self.hierarchyHeight() + 1);
        var from = Math.max(self.communityDrillLevel(), 1);
        return self.levelsArray().slice(from, self.hierarchyHeight() + 1);
    });


    // NODE LABELS ----------------------------------------------------------------------- //
    self.hasLabels = ko.observable(false);

    self.nodeDataChoice = ko.observable();

    self.availableNodeData = ko.computed(function() {
        self.nodeDataChoice('off');
        var options = [];
        options.push('off');
        if (self.graph()) {
            var metadata = self.graph().nodes[0].data.metadata;
            for (var key in metadata) {
               options.push(key);
            }
        }
        return options;
    });

    self.updateLabels = ko.computed(function() {
         if (self.hasLabels()) {
            self.cy().style().selector('node').css('content', 'data(metadata.' + self.nodeDataChoice() + ')').update();
        }
    });

    self.toggleLabels = function() {
        if (self.nodeDataChoice() === 'off') {
            self.cy().style().selector('node').css('content', '').update();
        } else {
            self.cy().style().selector('node').css('content', 'data(metadata.' + self.nodeDataChoice() + ')').update();
        }
    };

     self.refreshLabels = function() {
        if (self.hasLabels()) {
            self.cy().style().selector('node').css('content', 'data(metadata.' + self.nodeDataChoice() + ')').update();
        }
    };

    // LAYOUT OPTIONS ----------------------------------------------------------------------- //
    self.defaultOptions = {
        repulsion: 500,
        stiffness: 500,
        friction: 500,
        time: 10
    };

    self.layoutRepulsion = ko.observable(self.defaultOptions.repulsion);
    self.layoutStiffness = ko.observable(self.defaultOptions.stiffness);
    self.layoutFriction = ko.observable(self.defaultOptions.friction);
    self.layoutTime = ko.observable(self.defaultOptions.time);

    self.hasDefaultSettings = ko.computed( function() {
        return self.layoutRepulsion() === self.defaultOptions.repulsion
            && self.layoutStiffness() === self.defaultOptions.stiffness
            && self.layoutFriction() === self.defaultOptions.friction
            && self.layoutTime() === self.defaultOptions.time;
    });

    self.restoreDefaultSettings = function() {
        self.layoutRepulsion(self.defaultOptions.repulsion);
        self.layoutStiffness(self.defaultOptions.stiffness);
        self.layoutFriction(self.defaultOptions.friction);
        self.layoutTime(self.defaultOptions.time);
    }

    self.isArborRunning = ko.observable(false);

    self.cancelLayoutStatus = ko.observable(true);

    self.cancelLayout = function() {
        self.cancelLayoutStatus(true);
    };

    self.refreshLayout = function() {
        self.cancelLayoutStatus(false);
        self.cy().layout(arborLayout());
    };

    // UPLOAD ----------------------------------------------------------------------- //
    self.fileValue = ko.observable();

    self.hasAddedFile = ko.observable(false);

    self.intervalId = 0;

    self.uploadGraph = function() {
        if ($('#fileInput')[0].files[0]) {
            var filesize = $('#fileInput')[0].files[0].size;
            var algorithm = $('#algorithm').val();
            if (algorithm === 'ORCA' && filesize > MAX_FILESIZE_ORCA) {
                alertify.alert('This file is too big. Files larger than 10mb currently not supported by ORCA, try another algorithm.');
            } else if (filesize > MAX_FILESIZE) {
                alertify.alert('This file is too big. Files larger than 50mb currently not supported.');
            } else {
                if (filesize > FILESIZE_NEEDING_PROGRESSBAR) {
                    self.loadingGraph(true);
                    var percentComplete = 0;
                    progress(percentComplete, $('#progressBar'));
                    var increment = 5000000 / filesize;
                    self.intervalId = setInterval(function() {
                        percentComplete = percentComplete + increment;
                        progress(percentComplete, $('#progressBar'), 210);
                        var remaining = 100 - percentComplete;
                        if (remaining < 30) {
                            increment = Math.min(increment, remaining * 0.8);
                        }
                    }, 200);
                }
                self.selectedCommunity(-1);
                self.isShowingCommunity(false);
                var formData = new FormData($('form')[0]);
                graphRequest('ProcessGraph', formData, false, false, initialiseGraph);
            }
        } else {
            alertify.alert('Please choose a file to upload.');
        }        
    };

    self.updateGraphWithAjax = function(view) {
        var formData = 'graphLevel=' + view.drillLevel;
        formData += '&colourLevel=' + view.colourLevel;
        var dataFunction = initialiseGraph;
        var tooManyNodesToRecolour = self.graph().nodes.length >= 250
        if (self.currentLevel() === view.drillLevel && !tooManyNodesToRecolour) {
            dataFunction = refreshColours;
            formData += '&includeEdges=' + encodeURIComponent('false');
        } else {
            formData += '&includeEdges=' + encodeURIComponent('true');
        }
        self.selectedCommunity(-1);
        graphRequest('UpdateGraph', formData, true, 'application/x-www-form-urlencoded', dataFunction);
        self.currentViewTooBig(false);
        self.isShowingCommunity(false);
    };

    self.updateCommunityGraphWithAjax = function(view) {
        var formData = 'graphLevel=' + encodeURIComponent(view.drillLevel);
        formData += '&colourLevel=' + encodeURIComponent(view.colourLevel);
        formData += '&currentLevel=' + encodeURIComponent(view.currentLevel);
        formData += '&selectedNode=' + encodeURIComponent(view.community);
        formData += '&includeEdges=' + encodeURIComponent('true');
        self.selectedCommunity(-1);
        graphRequest('UpdateGraph', formData, true, 'application/x-www-form-urlencoded', initialiseGraph);
        self.currentViewTooBig(false);
        self.isShowingCommunity(true);
    };

    self.isCommunityTooBig = ko.observable(true);

    self.updateIsCommunityTooBig = ko.computed( function() {
        self.isCommunityTooBig(true);
        if (self.graph() && !self.isBottomLevel() && self.hasSelectedCommunity()) {
            var formData = 'graphLevel=' + encodeURIComponent(self.communityDrillLevel());
            formData += '&currentLevel=' + encodeURIComponent(self.currentLevel());
            formData += '&selectedNode=' + encodeURIComponent(self.selectedCommunity());
            $.ajax({
                type: 'GET',
                url: 'GetCommunitySize',
                data: formData,
                cache: false,
                success: function(data){
                    if (data < MAX_EDGES_VIEWABLE) {
                        self.isCommunityTooBig(false);
                    }
                }
            });
        }
    });

    self.updateGraph = function(obj, event) {
        if (event.originalEvent) {
            var view = new GraphView(self.drillLevel(), self.colourLevel(), self.currentLevel(), -1);
            if ((self.drillLevel() || self.drillLevel() === 0) && self.drillLevel() !== -1) {
                $.ajax({
                    type: 'GET',
                    url: 'GetLevelSize',
                    async: false,
                    data: 'graphLevel=' + encodeURIComponent(self.drillLevel()),
                    cache: false,
                    success: function(data){
                        if (data < MAX_NODES_VIEWABLE) {
                            self.viewHistory.push(view);
                            self.updateGraphWithAjax(view);
                        } else {
                            self.currentViewTooBig(true);
                        }
                    }
                });
            } else {
                self.currentViewTooBig(true);
            }
        }
    };

    self.updateCommunity = function() {
        var view = new GraphView(self.communityDrillLevel(), self.communityColourLevel(), self.currentLevel(), self.selectedCommunity());
        self.viewHistory.push(view);
        self.updateCommunityGraphWithAjax(view);
    };

    self.downloadGraph = function() {
        var formData = 'graphLevel=' + encodeURIComponent(self.drillLevel());
        formData += '&includeEdges=' + encodeURIComponent('true');
        if (self.isShowingCommunity()) {
            formData += '&selectedNode=' + self.currentCommunityShown();
            formData += '&colourLevel=' + encodeURIComponent(self.currentColourLevel());
            formData += '&currentLevel=' + encodeURIComponent(self.previousLevel());
        } else {
            formData += '&colourLevel=' + encodeURIComponent(self.colourLevel());
            formData += '&currentLevel=' + encodeURIComponent(self.currentLevel());
        }
        self.selectedCommunity(-1);
        window.location = 'DownloadGraph?' + formData;
    };

    self.downloadCommunity = function() {
        var formData = 'graphLevel=' + encodeURIComponent(self.communityDrillLevel());
        formData += '&colourLevel=' + encodeURIComponent(self.communityColourLevel());
        formData += '&currentLevel=' + encodeURIComponent(self.currentLevel());
        formData += '&selectedNode=' + encodeURIComponent(self.selectedCommunity());
        formData += '&includeEdges=' + encodeURIComponent('true');
        self.selectedCommunity(-1);
        window.location = 'DownloadGraph?' + formData;
    };

    // COMMUNITY TABLE ----------------------------------------------------------------------- //
    self.community = ko.observableArray();
    self.tableHeadings = ko.observableArray();

    self.computeTableHeadings = ko.computed(function() {
        var headings = [];
        if (self.community().length > 0 && self.community()[0].data.metadata) {
            var metadata = self.community()[0].data.metadata;
            for (var key in metadata) {
                if (key != 'community') {
                    headings.push(key);
                }
            }
        }
        self.tableHeadings(headings);
    });

    self.communityPageIndex = ko.observable(1);

    self.communityNoOfPages = ko.computed( function() {
        self.communityPageIndex(1);
        return Math.floor((self.selectedCommunitySize() - 1) / PAGE_SIZE) + 1;
    });

    self.isFirstPage = ko.computed( function() {
        return self.communityPageIndex() === 1;
    });

    self.isLastPage = ko.computed( function() {
        return self.communityPageIndex() === self.communityNoOfPages();
    });

    self.nextPage = function() {
        self.communityPageIndex(self.communityPageIndex() + 1);
    };

    self.previousPage = function() {
        self.communityPageIndex(self.communityPageIndex() - 1);
    };

    self.firstPage = function() {
        self.communityPageIndex(1);
    };

    self.lastPage = function() {
        self.communityPageIndex(self.communityNoOfPages());
    };

    self.loadingTable = ko.observable(true);

    self.updateCommunityTable = ko.computed(function() {
        if (self.hasSelectedCommunity() && !self.isBottomLevel() && self.drillLevel()) {
            self.loadingTable(true);
            var selectedCommunity = self.selectedCommunity();
            if (selectedCommunity !== -1) {
                var formData = 'graphLevel=' + encodeURIComponent(0);
                formData += '&colourLevel=' + encodeURIComponent(self.hierarchyHeight());
                formData += '&currentLevel=' + encodeURIComponent(self.currentLevel());
                formData += '&selectedNode=' + selectedCommunity;
                formData += '&includeEdges=' + encodeURIComponent('false');
                formData += '&offset=' + (self.communityPageIndex() - 1) * PAGE_SIZE;
                formData += '&size=' + PAGE_SIZE;
                graphRequest('GetCommunityNodes', formData, true, 'application/x-www-form-urlencoded', function(data) {
                    var page = data.nodes;
                    page.forEach( function (node) {
                        delete node.data.metadata.community;
                    });
                    self.community(page);
                    self.loadingTable(false);
                });
            }
        } else {
            self.community([]);
        }
    });

    // HELP ------------------------------------------------------------------------------- //
    self.showHelp = ko.observable(false);

    self.turnHelpOn = function() {
        self.showHelp(true);
        alertify.alert('Hover over the ? arrows to view help information.');
    };

    self.turnHelpOff = function() {
        self.showHelp(false);
    };
};

viewModel = new viewModel();

ko.applyBindings(viewModel);