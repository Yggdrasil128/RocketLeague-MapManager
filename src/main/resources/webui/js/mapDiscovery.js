let mapDiscoveryIntervalHandle = null;
let mapDiscoveryModalStatusContentsInitial = null;

function startMapDiscovery() {
    showMapDiscoveryModal();

    makeRequest('api/startMapDiscovery', null, null, function() {
        if(mapDiscoveryIntervalHandle == null) {
            mapDiscoveryIntervalHandle = setInterval(updateMapDiscoveryModal, 1000);
        }
    });
}

$(function() {
    mapDiscoveryModalStatusContentsInitial = $('#mapDiscoveryModal .status').html();

    // check if map discovery is running
    makeRequest('api/getMapDiscoveryStatus', null, null, function(data) {
        let json = JSON.parse(data);
        if(json['isDone']) {
            return;
        }

        showMapDiscoveryModal();

        if(mapDiscoveryIntervalHandle == null) {
            mapDiscoveryIntervalHandle = setInterval(updateMapDiscoveryModal, 1000);
        }
    });
});

function showMapDiscoveryModal() {
    $('#mapDiscoveryModal .status').html(mapDiscoveryModalStatusContentsInitial);
    $('#mapDiscoveryModal button').css('display', 'none');
    $('#mapDiscoveryModal').addClass('shown');
}

function hideMapDiscoveryModal() {
    $('#mapDiscoveryModal').removeClass('shown');
}

function updateMapDiscoveryModal() {
    makeRequest('api/getMapDiscoveryStatus', null, null, updateMapDiscoveryModalCallback);
}

function updateMapDiscoveryModalCallback(data) {
    let json = JSON.parse(data);

    if(json['isDone']) {
        clearInterval(mapDiscoveryIntervalHandle);
        mapDiscoveryIntervalHandle = null;

        $('#mapDiscoveryModal .status').html(json['message']);
        $('#mapDiscoveryModal button').css('display', '');

        loadMaps();

        return;
    }

    $('#mapDiscoveryModal .message').html(json['message']);
    $('#mapDiscoveryModal progress').attr({'value': json['progress'], 'max': json['progressTarget']});
    let s = json['progress'] + ' / ' + json['progressTarget'] + ' (' +
        (100 * json['progress'] / Math.max(json['progressTarget'], 1)).toFixed(0) +
        ' %)';
    $('#mapDiscoveryModal .progressText').html(s);
}
