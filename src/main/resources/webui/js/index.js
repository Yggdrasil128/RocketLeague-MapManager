let rlmmVersion = null;
let currentContentDiv = 'contentHome';
let status = null;
let statusUpdateIntervalHandle = null;
let disconnectedModalShown = false;
const browserTabID = getRandomString(8);

$(function() {
    // onclick handler for navbar
    $('#navBar .navList div.anchor').on('click', function() {
        const $this = $(this);
        if($this.hasClass('current')) {
            return;
        }
        currentContentDiv = $this.attr('data-divID');
        const $contentDiv = $('#' + currentContentDiv);

        $('#navBar .navList div.anchor.current, div.content.current').removeClass('current');
        $this.addClass('current');
        $contentDiv.addClass('current');

        updateSetupHints();
    });

    // clock
    setInterval(function() {
        const now = new Date();

        let i = now.getHours();
        let s = (i >= 10 ? i : '0' + i) + ":";
        i = now.getMinutes();
        s += (i >= 10 ? i : '0' + i) + ":";
        i = now.getSeconds();
        s += i >= 10 ? i : '0' + i;

        $('#localTime').html(s);
    }, 1000);

    // fetch version
    makeRequest('/api/getVersion', null, function(version) {
        rlmmVersion = version;
        $('#versionDiv').html('Version: ' + version);
    });

    makeRequest('/api/registerBrowserTab', browserTabID, function() {
        statusUpdateIntervalHandle = setInterval(updateStatus, 5000);
        updateStatus();
    });
});

function makeRequest(url, body, successCallback, errorCallback, timeoutMillis) {
    let request = new XMLHttpRequest();
    request.onload = function() {
        if(200 <= this.status && this.status <= 299) {
            if(successCallback) {
                successCallback(this.responseText);
            }
        } else {
            if(errorCallback) {
                errorCallback();
            }
        }
    };
    request.onerror = errorCallback;
    request.ontimeout = errorCallback;
    request.open(body ? "POST" : "GET", url, true);
    request.timeout = timeoutMillis ? timeoutMillis : 1000;
    if(body) {
        request.send(body);
    } else {
        request.send();
    }
}

function updateStatus() {
    makeRequest('api/getStatus', null, updateStatusCallback, updateStatusCallbackError);
}

function updateStatusCallback(data) {
    const oldStatus = status;
    status = JSON.parse(data);

    if(disconnectedModalShown) {
        location.reload();
        return;
    }

    if(status['currentBrowserTabID'] !== browserTabID) {
        $('#activeInAnotherTabModal').addClass('shown');
        clearInterval(statusUpdateIntervalHandle);
        statusUpdateIntervalHandle = null;
        return;
    }

    if(!oldStatus || oldStatus['isRLRunning'] ^ status['isRLRunning']) {
        if(status['isRLRunning']) {
            $('#rlStatus span').html('running').addClass('isRunning');
            $('#rlStatus button').html('Stop RL').attr('disabled', null);
        } else {
            $('#rlStatus span').html('not running').removeClass('isRunning');
            $('#rlStatus button').html('Start RL').attr('disabled', null);
        }
    }
}

function updateStatusCallbackError() {
    if(disconnectedModalShown) {
        return;
    }

    disconnectedModalShown = true;
    $('#disconnectModal').addClass('shown');
}

function startStopRocketLeague() {
    if(status['isRLRunning']) {
        $('#rlStatus button').html('Stopping...').attr('disabled', '');
        makeRequest('api/stopRocketLeague', null, updateStatus);
    } else {
        $('#rlStatus button').html('Starting...').attr('disabled', '');
        makeRequest('api/startRocketLeague', null, updateStatus);
    }
}

/**
 * @param a {*}
 * @param b {*}
 * @returns {*} the first argument if it is truthy, or else the second argument.
 */
function coalesce(a, b) {
    return a ? a : b;
}

function getRandomString(length) {
    let result = '';
// noinspection SpellCheckingInspection
    let characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let charactersLength = characters.length;
    for(let i = 0; i < length; i++) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
}