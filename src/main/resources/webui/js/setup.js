let mapDiscoveryUpdateIntervalHandle = null;
let hasWorkshopTextures = false;

$(function() {
    makeRequest('api/getAppPath', null, null, function(data) {
        $('#installationDirSpan').html(data);
    });
});

function getPlatform() {
    return $('#platform_epic').get(0).checked ? 1 : 0;
}

function setupPhase0() {
    $('div.content.current:not(#contentSetup0)').removeClass('current');
    $('#contentSetup0').addClass('current');
}

function setupPhase1() {
    $('div.content.current:not(#contentSetup1)').removeClass('current');
    $('#contentSetup1').addClass('current');
}

function setupPhase2() {
    gameDiscovery(true, true, function() {
        $('div.content.current:not(.contentSetup2)').removeClass('current');
        if(getPlatform() === 1) {
            $('#contentSetup2_epic').addClass('current');
        } else {
            $('#contentSetup2_steam').addClass('current');
        }
    }, true);
}

function setupPhase2_1() {
    makeRequest('api/workshopTextures_check', null, null, function(data) {
        hasWorkshopTextures = data === "1";
        if(hasWorkshopTextures) {
            setupPhase3();
        } else {
            $('div.content.current:not(#contentSetup2_1)').removeClass('current');
            $('#contentSetup2_1').addClass('current');
        }
    });
}

function setupPhase3() {
    makeRequest('api/getConfig', null, null, function(data) {
        const config = JSON.parse(data);

        $('#input_renameOriginalUPK').get(0).value = config['renameOriginalUnderpassUPK'] ? '1' : '0';
        $('#input_behaviorWhenRLIsStopped').get(0).value = config['behaviorWhenRLIsStopped'];
        $('#input_behaviorWhenRLIsRunning').get(0).value = config['behaviorWhenRLIsRunning'];

        $('div.content.current:not(#contentSetup3)').removeClass('current');
        $('#contentSetup3').addClass('current');
    });
}

function setupPhase4() {
    $('div.content.current:not(#contentSetup4)').removeClass('current');
    $('#contentSetup4').addClass('current');

    $('#installationStatus').html('Installing...');

    makeRequest('api/install', null, null, setupPhase4_callback1);
}

function setupPhase4_callback1() {
    $('#installationStatus').html('Configuring...');

    let config = {
        autostart: $('#input_autostart').get(0).value,
        renameOriginalUnderpassUPK: $('#input_renameOriginalUPK').get(0).value === '1',
        behaviorWhenRLIsStopped: $('#input_behaviorWhenRLIsStopped').get(0).value,
        behaviorWhenRLIsRunning: $('#input_behaviorWhenRLIsRunning').get(0).value,
    };

    makeRequest('api/patchConfig', null, JSON.stringify(config), setupPhase4_callback2);
}

function setupPhase4_callback2() {
    if($('#input_workshopTextures').get(0).value === '0') {
        setupPhase4_callback3();
        return;
    }

    makeRequest('api/workshopTextures_check', null, null, function(data) {
        if(data === "1") {
            setupPhase4_callback3();
            return;
        }

        let intervalHandle;

        let updateStatus = function(data2) {
            let status = JSON.parse(data2);
            let message = status['message'];
            $('#installationStatus').html('Installing workshop textures: ' + message);

            if(status['isFinished']) {
                clearInterval(intervalHandle);
                setupPhase4_callback3();
            }
        };

        makeRequest('api/workshopTextures_start', null, null, function(data) {
            intervalHandle = setInterval(function() {
                makeRequest('api/workshopTextures_status', null, null, updateStatus);
            }, 500);
            updateStatus(data);
        });

    });
}

function setupPhase4_callback3() {
    if(getPlatform() === 0) {
        $('#installationStatus').html('Discovering workshop maps...');

        makeRequest('api/startMapDiscovery', null, null, setupPhase4_callback4);
    } else {
        setupPhase5();
    }
}

function setupPhase4_callback4() {
    mapDiscoveryUpdateIntervalHandle = setInterval(setupPhase4_updateMapDiscoveryStatus, 1000);
}

function setupPhase4_updateMapDiscoveryStatus() {
    makeRequest('api/getMapDiscoveryStatus', null, null, setupPhase4_updateMapDiscoveryStatus_callback);
}

function setupPhase4_updateMapDiscoveryStatus_callback(data) {
    const status = JSON.parse(data);

    if(status['isFinished']) {
        clearInterval(mapDiscoveryUpdateIntervalHandle);
        mapDiscoveryUpdateIntervalHandle = null;

        setupPhase5();

        return;
    }

    const progress = status['progress'];
    const progressTarget = status['progressTarget'];

    if(progressTarget) {
        let s = progress + ' / ' + progressTarget + ' (' +
            (100 * progress / Math.max(progressTarget, 1)).toFixed(0) +
            ' %)';
        $('#installationStatus').html('Discovering workshop maps... ' + s);
    }
}

function setupPhase5() {
    $('div.content.current:not(#contentSetup5)').removeClass('current');
    $('#contentSetup5').addClass('current');
}

function setupPhase6() {
    $('#contentSetup5 button').attr('disabled', '');

    let startApp = $('#startRLMapManagerCheckbox').get(0).checked ? '1' : '0';
    let createDesktopShortcut = $('#createDesktopShortcutCheckbox').get(0).checked ? '1' : '0';

    makeRequest('api/exit', {startApp: startApp, createDesktopShortcut: createDesktopShortcut}, null, function() {
        window.close();
    });
}

function gameDiscovery(disableAlert, useDefaultDirectory, callback) {
    let $button = $('#contentSetup1 div.buttonContainer button');

    $button.attr('disabled', '');

    makeRequest(
        'api/gameDiscovery',
        {
            disableAlert: disableAlert ? '1' : '0',
            useDefaultDirectory: useDefaultDirectory ? '1' : '0',
            platform: getPlatform()
        },
        null,
        function(data) {
            $button.attr('disabled', null);

            if(!data) {
                return;
            }
            let result = JSON.parse(data);

            $('.steamappsFolder').html(coalesce(result['steamappsFolder'], '&mdash;'));
            $('.exeFile').html(coalesce(result['exeFile'], '&mdash;'));
            $('.upkFile').html(coalesce(result['upkFile'], '&mdash;'));
            $('.steamWorkshopFolder').html(coalesce(result['steamWorkshopFolder'], '&mdash;'));

            if(result['success']) {
                $('.gameDiscoveryError').css('display', 'none');
                $('.gameDiscoverySuccess').css('display', '');
                $('.toPhase2_1').attr('disabled', null);
            } else {
                $('.gameDiscoveryError').css('display', '');
                $('.gameDiscoverySuccess').css('display', 'none');
                $('.toPhase2_1').attr('disabled', '');
            }

            if(callback) {
                callback();
            }
        }, function() {
            $button.attr('disabled', null);
        }, 3600000);
}

function cancelInstallation() {
    let $buttons = $('button.cancelButton');
    $buttons.html('Cancelling...');

    let callback = function() {
        window.close();
    };

    makeRequest('api/cancel', null, null, callback, callback, 1000);
}
