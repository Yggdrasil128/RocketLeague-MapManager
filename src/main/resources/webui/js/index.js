let currentContentDiv = 'contentHome';
let status = null;
let statusUpdateIntervalHandle = null;
let disconnectedModalShown = false;
let lastUpdatedMapsTimestamp = 0;
let lastUpdatedConfigTimestamp = 0;

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

    statusUpdateIntervalHandle = setInterval(updateStatus, 5000);
    updateStatus();
});

function updateStatus() {
    makeRequest('api/getStatus', null, null, updateStatusCallback, updateStatusCallbackError);
}

function updateStatusCallback(data) {
    const oldStatus = status;
    status = JSON.parse(data);

    if(disconnectedModalShown) {
        location.reload();
        return;
    }

    if(status['lastUpdatedMaps']['browserTabID'] !== browserTabID && status['lastUpdatedMaps']['timestamp'] > lastUpdatedMapsTimestamp) {
        lastUpdatedMapsTimestamp = status['lastUpdatedMaps']['timestamp'];
        console.log('Reloading maps');
        loadMaps();
    }

    if(status['lastUpdatedConfig']['browserTabID'] !== browserTabID && status['lastUpdatedConfig']['timestamp'] > lastUpdatedConfigTimestamp) {
        lastUpdatedConfigTimestamp = status['lastUpdatedConfig']['timestamp'];
        console.log('Reloading config');
        loadConfig(function() {
            loadMapSortingSettingsFromConfig();
            updateMapComparator();
            sortMaps();
            refreshMapView();
        });
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
        makeRequest('api/stopRocketLeague', null, null, updateStatus);
    } else {
        $('#rlStatus button').html('Starting...').attr('disabled', '');
        makeRequest('api/startRocketLeague', null, null, updateStatus);
    }
}
