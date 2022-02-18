// noinspection JSCheckFunctionSignatures

function handleTask(taskDivID, doStartTask = true, onFinishCallback = null) {
    let taskDiv = $('#' + taskDivID);
    let startButton = taskDiv.find('button.start');
    let statusDiv = taskDiv.find('.statusDiv');
    let statusSpan = statusDiv.find('.status');
    let cancelButton = statusDiv.find('button');

    startButton.attr('disabled', '');
    cancelButton.attr('disabled', null);
    statusSpan.html('').css('color', '#c2ffbb');
    statusDiv.css('display', '');

    let apiEndpointName = taskDiv.data('api-endpoint-name');
    tasksRunning[apiEndpointName] = true;

    let intervalHandle;

    let updateStatus = function(data) {
        let status = JSON.parse(data);
        statusSpan.html(status['message']);
        if(status['isFinished']) {
            clearInterval(intervalHandle);

            tasksRunning[apiEndpointName] = false;

            cancelButton.attr('disabled', '');
            startButton.attr('disabled', null);
            if(status['message'].startsWith('Error')) {
                statusSpan.css('color', '#ffbbbb');
            }

            if(onFinishCallback) {
                onFinishCallback();
            }
        }
    };
    if(!doStartTask) {
        intervalHandle = setInterval(function() {
            makeRequest('api/' + apiEndpointName + '_status', null, null, updateStatus);
        }, 500);
        return;
    }

    let startCallback = function(data) {
        intervalHandle = setInterval(function() {
            makeRequest('api/' + apiEndpointName + '_status', null, null, updateStatus);
        }, 500);
        updateStatus(data);
    };

    let params = null;
    let input = taskDiv.find('input');
    if(input.length > 0) {
        params = {url: input.val()};
    }

    makeRequest('api/' + apiEndpointName + '_start', params, null, startCallback, function() {
        statusSpan.html('Error: Unable to start the task.').css('color', '#ffbbbb');
        cancelButton.attr('disabled', '');
        startButton.attr('disabled', null);
    });
}

function uploadMap(buttonElement) {
    let startButton = $(buttonElement);
    let importOptionDiv = $(buttonElement.parentElement);
    let statusDiv = importOptionDiv.find('.statusDiv');
    let statusSpan = statusDiv.find('.status');

    startButton.attr('disabled', '');
    statusDiv.css('display', 'none');

    let callback = function(response) {
        startButton.attr('disabled', null);
        if(!response) {
            return;
        }
        statusSpan.html(response).css('color', response.startsWith('Error') ? '#ffbbbb' : '#c2ffbb');
        statusDiv.css('display', '');

        loadMaps();
    };

    makeRequest('api/importCustomMap', null, null, callback, callback, 3600000);
}
