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
    let cancelButton = statusDiv.find('button');

    startButton.attr('disabled', '');
    cancelButton.attr('disabled', null);
    statusSpan.html('Importing...').css('color', '#c2ffbb');
    statusDiv.css('display', '');

    let callback = function(response) {
        statusSpan.html(response);
        cancelButton.attr('disabled', '');
        startButton.attr('disabled', null);
        if(response.startsWith('Error')) {
            statusSpan.css('color', '#ffbbbb');
        }

        loadMaps();
    };

    let files = $('#fromFile_input').get(0).files;
    if(files.length === 0) {
        callback("Error: Please select a UDK/UPK file or a ZIP file containing a UDK/UPK file.");
        return;
    }
    let file = files[0];

    let fileReader = new FileReader();
    fileReader.onload = function(event) {
        let data = event.target.result;
        makeRequest('api/upload/map', {filename: file.name}, data, callback);
    };
    fileReader.readAsArrayBuffer(file);
}
