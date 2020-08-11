function loadUpdateInfo() {
    makeRequest('api/getUpdateInfo', null, null, loadUpdateInfoCallback);
}

function loadUpdateInfoCallback(json) {
    const info = JSON.parse(json);

    $('#contentUpdate > h1').html(info['name']);
    const converter = new showdown.Converter();
    $('#contentUpdate > p').html(converter.makeHtml(info['body']));
    $('#contentUpdate > a').attr('href', info['html_url']);
}

function installUpdate() {
    $('#contentUpdate > button').html('Installing update...').attr('disabled', '');

    makeRequest('api/installUpdate', null, null, installUpdateCallback, installUpdateCallback);
}

function installUpdateCallback() {
    setInterval(function() {
        makeRequest('api/getVersion', null, null, function() {
            window.close();
            $('#contentUpdate > button').html('Update installed!...');
            location.reload();
        }, null, 1000);
    }, 1000);

}
