let mapDiscoveryIntervalHandle = null;

function startMapDiscovery() {
    showMapDiscoveryModal();

    makeRequest('api/startMapDiscovery', '', function() {
        if(mapDiscoveryIntervalHandle == null) {
            mapDiscoveryIntervalHandle = setInterval(updateMapDiscoveryModal, 1000);
        }
    });
}

$(function() {
    // check if map discovery is running
    makeRequest('api/getMapDiscoveryStatus', '', function(data) {
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
    $('#mapDiscoveryModal .message').html('Discovering maps, please wait...<br />0 %');
    $('#mapDiscoveryModal button').css('display', 'none');
    $('#mapDiscoveryModal').addClass('shown');
}

function hideMapDiscoveryModal() {
    $('#mapDiscoveryModal').removeClass('shown');
}

function updateMapDiscoveryModal() {
    makeRequest('api/getMapDiscoveryStatus', '', updateMapDiscoveryModalCallback);
}

function updateMapDiscoveryModalCallback(data) {
    let json = JSON.parse(data);

    const $messageDiv = $('#mapDiscoveryModal .message');

    if(json['isDone']) {
        clearInterval(mapDiscoveryIntervalHandle);
        mapDiscoveryIntervalHandle = null;

        $messageDiv.html(json['message']);
        $('#mapDiscoveryModal button').css('display', '');

        loadMaps();

        return;
    }

    let s = json['message'];
    s += '<br />';
    s += (100 * json['progress']).toFixed(0);
    s += ' %';
    $messageDiv.html(s);

}