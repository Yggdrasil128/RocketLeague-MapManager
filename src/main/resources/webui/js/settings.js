let config = null;

$(function() {
    loadConfig(function() {
        loadMapSortingSettingsFromConfig();
        updateMapComparator();

        loadMaps();
    });

    makeRequest('api/hasDesktopIcon', null, null, function(data) {
        if(data !== '1') {
            return;
        }
        $('#createDesktopIconButton').css('display', 'none');
    })
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
    $('#steamappsFolder').html(coalesce(config['paths']['steamappsFolder'], '&mdash;'));
    $('#exeFile').html(coalesce(config['paths']['exeFile'], '&mdash;'));
    $('#upkFile').html(coalesce(config['paths']['upkFile'], '&mdash;'));
    $('#workshopFolder').html(coalesce(config['paths']['workshopFolder'], '&mdash;'));

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

function chooseSteamappsFolder() {
    let $button = $('#steamLibrarySetupDiv button');

    $button.attr('disabled', '');

    makeRequest('api/chooseSteamLibrary', null, null, function(data) {
        $button.attr('disabled', null);

        if(!data) {
            return;
        }
        let result = JSON.parse(data);
        if(result['success']) {
            loadConfig();
            startMapDiscovery();
        }

    }, function() {
        $button.attr('disabled', null);
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

function createDesktopIcon() {
    makeRequest('api/createDesktopIcon', null, null, function() {
        const $button = $('#createDesktopIconButton');
        $button.html('Done');
        setTimeout(function() {
            $button.css('display', 'none');
        }, 3000);
    });
}
