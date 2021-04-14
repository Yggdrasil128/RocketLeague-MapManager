let editingMapID = null;

function startEditMap(mapID) {
    $('#navBar .navList div.anchor.current, div.content.current').removeClass('current');
    $('#contentEditMap').addClass('current');

    editingMapID = mapID;
    let map = getMapDataById(mapID);

    $('#editMap_image').val(null);
    $('#editMap_title').val(map['title']);
    $('#editMap_authorName').val(map['authorName']);
    $('#editMap_description').val(map['description'].replaceAll(/<br *\/?>/ig, "\n"));
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
                    button.html('Error: Couldn\'t store map image');
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
