let mapDiscoveryUpdateIntervalHandle = null;

$(function() {
    setupPhase0();
});

function setupPhase0() {
    makeRequest('api/getAppPath', null, null, function(data) {
        $('#installationDirSpan').html(data);

        $('div.content.current:not(#contentSetup0)').removeClass('current');
        $('#contentSetup0').addClass('current');
    });
}

function setupPhase1() {
    steamLibraryDiscovery(null, function() {
        $('div.content.current:not(#contentSetup1)').removeClass('current');
        $('#contentSetup1').addClass('current');
    }, true);
}

function setupPhase2() {
    makeRequest('api/getConfig', null, null, function(data) {
        const config = JSON.parse(data);

        $('#input_renameOriginalUPK').get(0).value = config['renameOriginalUnderpassUPK'] ? '1' : '0';
        $('#input_behaviorWhenRLIsStopped').get(0).value = config['behaviorWhenRLIsStopped'];
        $('#input_behaviorWhenRLIsRunning').get(0).value = config['behaviorWhenRLIsRunning'];

        $('div.content.current:not(#contentSetup2)').removeClass('current');
        $('#contentSetup2').addClass('current');
    });
}

function setupPhase3() {
    $('div.content.current:not(#contentSetup3)').removeClass('current');
    $('#contentSetup3').addClass('current');

    $('#installationStatus').html('Installing...');

    makeRequest('api/install', null, null, setupPhase3_callback1);
}

function setupPhase3_callback1() {
    $('#installationStatus').html('Configuring...');

    let config = {
        autostart: $('#input_autostart').get(0).value,
        renameOriginalUnderpassUPK: $('#input_renameOriginalUPK').get(0).value === '1',
        behaviorWhenRLIsStopped: $('#input_behaviorWhenRLIsStopped').get(0).value,
        behaviorWhenRLIsRunning: $('#input_behaviorWhenRLIsRunning').get(0).value,
    };

    makeRequest('api/patchConfig', null, JSON.stringify(config), setupPhase3_callback2);
}

function setupPhase3_callback2() {
    $('#installationStatus').html('Discovering maps...');

    makeRequest('api/startMapDiscovery', null, null, setupPhase3_callback3);
}

function setupPhase3_callback3() {
    mapDiscoveryUpdateIntervalHandle = setInterval(setupPhase3_updateMapDiscoveryStatus, 1000);
}

function setupPhase3_updateMapDiscoveryStatus() {
    makeRequest('api/getMapDiscoveryStatus', null, null, setupPhase3_updateMapDiscoveryStatus_callback);
}

function setupPhase3_updateMapDiscoveryStatus_callback(data) {
    const status = JSON.parse(data);

    if(status['isDone']) {
        clearInterval(mapDiscoveryUpdateIntervalHandle);
        mapDiscoveryUpdateIntervalHandle = null;

        setupPhase4();

        return;
    }

    const progress = status['progress'];
    const progressTarget = status['progressTarget'];

    $('#contentSetup3 progress').attr({'value': progress, 'max': progressTarget});
    let s = progress + ' / ' + progressTarget + ' (' +
        (100 * progress / Math.max(progressTarget, 1)).toFixed(0) +
        ' %)';
    $('#contentSetup3 .progressText').html(s);
}

function setupPhase4() {
    $('div.content.current:not(#contentSetup4)').removeClass('current');
    $('#contentSetup4').addClass('current');
}

function setupPhase5() {
    $('#contentSetup4 button').attr('disabled', '');

    let startApp = $('#startRLMapManagerCheckbox').get(0).checked ? '1' : '0';

    makeRequest('api/exit', {startApp: startApp}, null, function() {
        window.close();
    });
}

function steamLibraryDiscovery(path, callback, disableAlert) {
    makeRequest('api/steamLibraryDiscovery', null, path, function(data) {
        const result = JSON.parse(data);

        $('#steamappsFolder').html(coalesce(result['steamappsFolder'], '&mdash;'));
        $('#exeFile').html(coalesce(result['exeFile'], '&mdash;'));
        $('#upkFile').html(coalesce(result['upkFile'], '&mdash;'));
        $('#workshopFolder').html(coalesce(result['workshopFolder'], '&mdash;'));

        if(result['success']) {
            $('#couldNotFindSteamappsFolder').css('display', 'none');
            $('#steamappsFolderIsConfigured').css('display', '');
            $('#toPhase2').attr('disabled', null);
        } else {
            $('#couldNotFindSteamappsFolder').css('display', '');
            $('#steamappsFolderIsConfigured').css('display', 'none');
            $('#toPhase2').attr('disabled', '');

            if(!disableAlert) {
                alert('Error: ' + result['message']);
            }
        }

        if(callback) {
            callback();
        }
    });
}

function chooseSteamappsFolder() {
    const path = prompt("Please enter the path to the steamapps folder where Rocket League is installed. For example: C:\\Program Files (x86)\\Steam\\steamapps");
    if(!path || path === '') {
        return;
    }

    steamLibraryDiscovery(path);
}
