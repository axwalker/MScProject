$(function() {
    var DisplayForm = Backbone.Model.extend({
        schema: {
            algorithm: { 
                type: 'Select', 
                options: ['Label Propagation', 'Louvain', 'ORCA']
            },
            minCommunitySize: 'Number'
        },
        submitButton: 'Submit!'
    });
    
    var displayForm = new DisplayForm({
        algorithm: 'Label Propagation',
        minCommunitySize: 10
    });
    
    var form = new Backbone.Form({
        model: displayForm
    }).render();

    $('#LiveDisplayForm').append(form.el);

    $('#liveSubmit').click(function() {
        form.commit();
        window.alert('Button was pressed');
    });
});