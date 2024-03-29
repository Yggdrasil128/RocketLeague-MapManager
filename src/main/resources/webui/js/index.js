let currentContentDiv = 'contentMapList';
let status = null;
let statusUpdateIntervalHandle = null;
let disconnectedModalShown = false;
let lastUpdatedMapsTimestamp = 0;
let lastUpdatedConfigTimestamp = 0;
let tasksRunning = {
    steamWorkshopMapDiscovery: false,
    steamWorkshopMapDownload: false,
    lethamyrMapDiscovery: false,
};

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
        loadMaps();
    }

    if(status['lastUpdatedConfig']['browserTabID'] !== browserTabID && status['lastUpdatedConfig']['timestamp'] > lastUpdatedConfigTimestamp) {
        lastUpdatedConfigTimestamp = status['lastUpdatedConfig']['timestamp'];
        loadConfig(function() {
            loadMapListSettingsFromConfig();
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

    if(!oldStatus || oldStatus['updateAvailable'] !== status['updateAvailable'] && status['updateAvailable'] !== null) {
        if(status['updateAvailable']) {
            $('#versionDiv .latest').css('display', 'none');
            $('#versionDiv .updateAvailable').css('display', '');

            $('div.anchor[data-divID=contentUpdate]').css('display', '');

            loadUpdateInfo();
        } else {
            $('#versionDiv .latest').css('display', '');
            $('#versionDiv .updateAvailable').css('display', 'none');

            $('div.anchor[data-divID=contentUpdate]').css('display', 'none');

            if(currentContentDiv === 'contentUpdate') {
                $('div.anchor[data-divID=contentMapList]').trigger('click');
            }
        }
    }

    if(status['tasksRunning']['steamWorkshopMapDiscovery'] && !tasksRunning['steamWorkshopMapDiscovery']) {
        handleTask('fromSteamWorkshopDirect', false, loadMaps);
    }
    if(status['tasksRunning']['steamWorkshopMapDownload'] && !tasksRunning['steamWorkshopMapDownload']) {
        handleTask('fromSteamWorkshopURL', false, loadMaps);
    }
    if(status['tasksRunning']['lethamyrMapDownload'] && !tasksRunning['lethamyrMapDownload']) {
        handleTask('fromLethamyrURL', false, loadMaps);
    }
    if(status['tasksRunning']['workshopTextures'] && !tasksRunning['workshopTextures']) {
        handleTask('workshopTextures', false, checkWorkshopTextures);
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

window.addEventListener("keydown", function(e) {
    if((e.ctrlKey && e.key === "f") || e.key === "F3") {
        if(currentContentDiv === 'contentMapList' && $('#mapSearchFocus').get(0).checked) {
            let mapSearch = $('#mapSearch').get(0);
            mapSearch.focus();
            mapSearch.select();
            e.preventDefault();
        }
    }
});
