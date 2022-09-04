let editingMapID = null;
let mapCanBeDeleted = false;

function startEditMap(mapID) {
    $('#navBar .navList div.anchor.current, div.content.current').removeClass('current');
    $('#contentEditMap').addClass('current');

    editingMapID = mapID;
    let map = getMapDataById(mapID);

    $('#editMap_image').val(null);
    $('#editMap_title').val(map['title']);
    $('#editMap_authorName').val(map['authorName']);
    $('#editMap_description').val(map['description'].replaceAll(/<br *\/?>/ig, "\n"));

    updateRefreshMetadataButton();

    mapCanBeDeleted = map['canBeDeleted'];
    updateDeleteMapButton();
}

function saveEditMap(skipImageUpload = false) {
    let button = $('#mapEditSubmitButton');

    if(!skipImageUpload) {
        let files = $('#editMap_image').get(0).files;
        if(files.length > 0) {
            let file = files[0];

            let fileReader = new FileReader();
            fileReader.onload = function(event) {
                let data = event.target.result;
                let params = {
                    mapID: editingMapID,
                    filename: file.name,
                    mimeType: file.type
                };
                makeRequest('api/upload/mapImage', params, data, function() {
                    saveEditMap(true);
                }, function() {
                    button.html("Error: Couldn't store map image");
                    setTimeout(function() {
                        button.html('Save changes');
                    }, 3000);
                });
            };
            fileReader.readAsArrayBuffer(file);
            return;
        }
    }

    let params = {
        mapID: editingMapID,
        title: $('#editMap_title').val(),
        authorName: $('#editMap_authorName').val(),
        description: $('#editMap_description').val(),
    };

    makeRequest('api/editMap', params, null, function() {
        button.html('Done!');
        setTimeout(function() {
            button.html('Save changes');
        }, 3000);

        loadMaps();
    }, function() {
        button.html('Error: Couldn\'t store map data');
        setTimeout(function() {
            button.html('Save changes');
        }, 3000);
    });
}

function updateDeleteMapButton() {
    let deleteMapButton = $('#deleteMapButton');
    if(mapCanBeDeleted) {
        deleteMapButton.attr('disabled', null);
        deleteMapButton.attr('title', '');
    } else {
        deleteMapButton.attr('disabled', '');
        deleteMapButton.attr('title', 'This map cannot be deleted.');
    }
}

function updateRefreshMetadataButton() {
    let button = $('#mapEditRefreshButton');
    if(editingMapID.startsWith('S-')) {
        button.html('Refresh data from Steam workshop page').css('display', '');
    } else if(editingMapID.startsWith('L-')) {
        button.html('Refresh data from lethamyr.com page').css('display', '');
    } else {
        button.css('display', 'none');
    }
}

function refreshDataFromSourcePage() {
    let buttons = $('#contentEditMap button');
    let button = $('#mapEditRefreshButton');

    buttons.attr('disabled', '');
    button.html('Please wait...');

    makeRequest('api/refreshMapMetadata', {mapID: editingMapID}, null, function(data) {
        if(data.startsWith('Error: ')) {
            button.html(data);
        } else {
            button.html('Done!');
            let map = JSON.parse(data);
            $('#editMap_image').val(null);
            $('#editMap_title').val(map['title']);
            $('#editMap_authorName').val(map['authorName']);
            $('#editMap_description').val(map['description'].replaceAll(/<br *\/?>/ig, "\n"));
            loadMaps();
        }
        setTimeout(updateRefreshMetadataButton, 3000);
        buttons.attr('disabled', null);
        updateDeleteMapButton();
    }, function() {
        button.html('Error: Couldn\'t load map data from page');
        setTimeout(updateRefreshMetadataButton, 3000);
        buttons.attr('disabled', null);
        updateDeleteMapButton();
    }, 10000);
}

function deleteMap() {
    if(!confirm("Are you sure you want to delete this map?")) {
        return;
    }
    let button = $('#deleteMapButton');
    makeRequest('api/deleteMap', {mapID: editingMapID}, null, function() {
        loadMaps();
        $('#navBar .navList div.anchor.current, div.content.current').removeClass('current');
        $('[data-divID=contentMapList], #contentMapList').addClass('current');
    }, function() {
        button.html('Error: Couldn\'t delete map');
        setTimeout(function() {
            button.html('Delete map');
        }, 3000);
    });
}
