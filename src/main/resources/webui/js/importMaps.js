function importMapsTask(buttonElement, inputElementID, apiEndpointName) {
    let startButton = $(buttonElement);
    let importOptionDiv = $(buttonElement.parentElement);
    // noinspection JSCheckFunctionSignatures
    let statusDiv = importOptionDiv.find('.statusDiv');
    // noinspection JSCheckFunctionSignatures
    let statusSpan = statusDiv.find('.status');
    let cancelButton = statusDiv.find('button');

    startButton.attr('disabled', '');
    cancelButton.attr('disabled', null);
    statusSpan.html('').css('color', '#c2ffbb');
    statusDiv.css('display', '');

    let intervalHandle;

    let updateStatus = function(data) {
        let status = JSON.parse(data);
        statusSpan.html(status['message']);
        if(status['isFinished']) {
            clearInterval(intervalHandle);

            cancelButton.attr('disabled', '');
            startButton.attr('disabled', null);
            if(status['message'].startsWith('Error')) {
                statusSpan.css('color', '#ffbbbb');
            }

            loadMaps();
        }
    };
    let startCallback = function(data) {
        intervalHandle = setInterval(function() {
            makeRequest('api/' + apiEndpointName + '_status', null, null, updateStatus);
        }, 500);
        updateStatus(data);
    };

    let params = null;
    if(inputElementID != null) {
        let input = $('#' + inputElementID);
        let type = input.attr('type');
        if(type === 'text') {
            params = {url: input.val()}
        }
    }

    makeRequest('api/' + apiEndpointName + '_start', params, null, startCallback);
}
