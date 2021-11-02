let config = null;

$(function() {
    loadConfig(function() {
        loadMapListSettingsFromConfig();
        updateMapComparator();

        loadMaps();
    });

    checkDesktopIcon();
    checkWorkshopTextures();
});

function loadConfig(callback) {
    makeRequest('api/getConfig', null, null, function(data) {
        config = JSON.parse(data);
        fillFromConfig();
        if(callback) {
            callback();
        }
    });
}

function fillFromConfig() {
    if(config['platform'] === 0) {
        // Steam
        $('#platform td:nth-child(2)').html('Steam');
        $('#switchPlatforms').html('Switch to Epic Games');
        $('#selectGameFolder').html('Choose steamapps folder');

        $('#steamappsFolder').css('display', '');
        $('#steamappsFolder td:nth-child(2)').html(coalesce(config['paths']['steamappsFolder'], '&mdash;'));
        $('#workshopFolder').css('display', '');
        $('#workshopFolder td:nth-child(2)').html(coalesce(config['paths']['workshopFolder'], '&mdash;'));

        $('#fromSteamWorkshopDirect').css('display', '');
        $('#fromSteamWorkshopURL').css('display', 'none');
    } else {
        // Epic Games
        $('#platform td:nth-child(2)').html('Epic Games');
        $('#switchPlatforms').html('Switch to Steam');
        $('#selectGameFolder').html('Choose Epic Games folder');

        $('#steamappsFolder').css('display', 'none');
        $('#workshopFolder').css('display', 'none');

        $('#fromSteamWorkshopDirect').css('display', 'none');
        $('#fromSteamWorkshopURL').css('display', '');
    }

    $('#exeFile td:nth-child(2)').html(coalesce(config['paths']['exeFile'], '&mdash;'));
    $('#upkFile td:nth-child(2)').html(coalesce(config['paths']['upkFile'], '&mdash;'));

    $('#input_autostart').get(0).value = config['autostart'];
    $('#input_renameOriginalUnderpassUPK').get(0).value = config['renameOriginalUnderpassUPK'] ? '1' : '0';
    $('#input_behaviorWhenRLIsStopped').get(0).value = config['behaviorWhenRLIsStopped'];
    $('#input_behaviorWhenRLIsRunning').get(0).value = config['behaviorWhenRLIsRunning'];

    $('#input_upkFilename').get(0).value = config['upkFilename'];
    $('#input_webInterfacePort').get(0).value = config['webInterfacePort'];
    $('#input_ipWhitelist').get(0).value = config['ipWhitelist'];

    $('#settingsDiv').css('display', config['needsSetup'] ? 'none' : '');
    $('.showOnlyWhenSetupIsNeeded').css('display', config['needsSetup'] ? 'block' : '');

    updateSetupHints();

    $('#settingsDiv .buttonContainer button').attr('disabled', '');
    $('#unsavedSettings').css('display', 'none');
}

function onSettingsChanges() {
    $('#settingsDiv .buttonContainer button').attr('disabled', null);
    $('#unsavedSettings').css('display', '');
}

function storeConfig() {
    let json = {};

    json['autostart'] = parseInt($('#input_autostart').get(0).value);
    json['renameOriginalUnderpassUPK'] = $('#input_renameOriginalUnderpassUPK').get(0).value === '1';
    json['behaviorWhenRLIsStopped'] = parseInt($('#input_behaviorWhenRLIsStopped').get(0).value);
    json['behaviorWhenRLIsRunning'] = parseInt($('#input_behaviorWhenRLIsRunning').get(0).value);

    json['upkFilename'] = $('#input_upkFilename').get(0).value;
    json['webInterfacePort'] = parseInt($('#input_webInterfacePort').get(0).value);
    json['ipWhitelist'] = $('#input_ipWhitelist').get(0).value;

    makeRequest('api/patchConfig', null, JSON.stringify(json), function() {
        loadConfig(function() {
            let $button = $('#settingsSubmitButton');
            $button.html('Settings stored!');
            setTimeout(function() {
                $button.html('Store settings');
            }, 3000);
        });
    });
}

function gameDiscovery(switchPlatforms) {
    let $buttons = $('#gameLibrarySetupDiv button');

    $buttons.attr('disabled', '');

    let params;
    if(switchPlatforms) {
        params = {
            platform: 1 - config['platform'],
            tryDefaultDirectoryFirst: '1'
        }
    } else {
        params = {
            platform: config['platform'],
            tryDefaultDirectoryFirst: '0'
        }
    }

    makeRequest('api/gameDiscovery', params, null, function(data) {
        $buttons.attr('disabled', null);

        if(!data) {
            return;
        }
        let result = JSON.parse(data);
        if(result['success']) {
            loadConfig();
        }

    }, function() {
        $buttons.attr('disabled', null);
    }, 3600000);
}

function updateSetupHints() {
    if(!config) {
        return;
    }
    let $setupHint = $('.setupHint');
    $setupHint.removeClass('setupHintShown');
    if(!config['needsSetup']) {
        return;
    }

    if(currentContentDiv !== 'contentSettings') {
        $setupHint.addClass('setupHintShown');
    }
}

function exitApp() {
    let $button = $('#exitButton');
    $button.html('Exiting...');

    let callback = function() {
        window.close();
    };

    makeRequest('api/exitApp', null, null, callback, callback, 1000);
}

function checkDesktopIcon() {
    makeRequest('api/hasDesktopIcon', null, null, function(data) {
        if(data !== '1') {
            return;
        }
        $('#createDesktopIconButton').css('display', 'none');
    });
}

function createDesktopIcon() {
    makeRequest('api/createDesktopIcon', null, null, function() {
        const $button = $('#createDesktopIconButton');
        $button.html('Done');
        setTimeout(function() {
            $button.css('display', 'none');
        }, 3000);
    });
}

function checkWorkshopTextures() {
    makeRequest('api/workshopTextures_check', null, null, function(data) {
        const result = data === "1";
        $('#workshopTextures .yes').css('display', result ? '' : 'none');
        $('#workshopTextures .no').css('display', result ? 'none' : '');
    });
}
