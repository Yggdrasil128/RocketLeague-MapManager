let config = null;

$(function() {
    initConfig(function() {
        // load layout and sorting options from config
        $('#mapLayoutSelect').get(0).value = config['mayLayout'];
        $('#mapSortingSelect').get(0).value = config['mapSorting'];
        $('#mapSorting_loadedMapAtTop').get(0).checked = config['showLoadedMapAtTop'];
        $('#mapSorting_favoritesAtTop').get(0).checked = config['showFavoritesAtTop'];
        updateMapComparator();

        loadMaps();
    });
});

function initConfig(callback) {
    makeRequest('api/getConfig', null, function(data) {
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

    $('#input_renameOriginalUPK').get(0).value = config['renameOriginalUnderpassUPK'] ? '1' : '0';
    $('#input_behaviorWhenRLIsStopped').get(0).value = config['behaviorWhenRLIsStopped'];
    $('#input_behaviorWhenRLIsRunning').get(0).value = config['behaviorWhenRLIsRunning'];

    $('#input_upkFilename').get(0).value = config['upkFilename'];
    $('#input_webInterfacePort').get(0).value = config['webInterfacePort'];

    $('#settingsDiv').css('display', config['needsSetup'] ? 'none' : '');
    $('.showOnlyWhenSetupIsNeeded').css('display', config['needsSetup'] ? 'block' : '');

    updateSetupHints();
}

function storeConfig() {
    let json = {};

    // noinspection EqualityComparisonWithCoercionJS
    json['renameOriginalUPK'] = $('#input_renameOriginalUPK').get(0).value == '1';
    json['behaviorWhenRLIsStopped'] = parseInt($('#input_behaviorWhenRLIsStopped').get(0).value);
    json['behaviorWhenRLIsRunning'] = parseInt($('#input_behaviorWhenRLIsRunning').get(0).value);

    json['upkFilename'] = $('#input_upkFilename').get(0).value;
    json['webInterfacePort'] = parseInt($('#input_webInterfacePort').get(0).value);

    console.log(JSON.stringify(json));

    makeRequest('api/patchConfig', JSON.stringify(json), function() {
        initConfig(function() {
            let $button = $('#settingsSubmitButton');
            $button.html('Settings stored!');
            setTimeout(function() {
                $button.html('Store settings');
            }, 3000);
        });
    });
}

function chooseSteamappsFolder() {
    const path = prompt("Please enter the path to the steamapps folder where Rocket League is installed. For example: C:\\Program Files (x86)\\Steam\\steamapps");
    if(!path || path === '') {
        return;
    }

    makeRequest('api/discoverSteamLibrary', path, function(data) {
        let result = JSON.parse(data);
        if(result['success']) {
            initConfig();
            alert('Steam Library successfully configured.');
            startMapDiscovery();
        } else {
            alert('Error: ' + result['message']);
        }
    }, function() {
        alert('An error occurred. Please check the log for details about the error.');
    });
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

    if(currentContentDiv !== 'contentConfiguration') {
        $setupHint.addClass('setupHintShown');
    }
}

function exitApp() {
    let $button = $('#exitButton');
    $button.html('Exiting...');

    makeRequest('api/exitApp', null, null, function() {
        window.close();
    }, 1000);
}