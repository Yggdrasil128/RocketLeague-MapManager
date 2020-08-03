let maps = [];
let loadedMapID = '0';
let lastPlayedFormatter = new Intl.DateTimeFormat(undefined, {year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'});

function getMapDataById(mapID) {
    for(let map of maps) {
        if(map['id'] === mapID) {
            return map;
        }
    }
    return null;
}

function loadMaps() {
    makeRequest('api/getMaps', null, loadMapsCallback);
}

function loadMapsCallback(data) {
    maps = JSON.parse(data);
    makeRequest('api/getLoadedMapID', null, function(data) {
        loadedMapID = data;
        refreshMapView();
    });
}

function refreshMapView() {
    let html = '<table class="maps">';

    for(const map of maps) {
        let mapID = map['id'];
        let mapIDStr = "'" + mapID + "'";
        let thisMapIsLoaded = loadedMapID === mapID;

        if(thisMapIsLoaded) {
            html += '<tr id="map' + mapID + '" class="loaded">';
        } else {
            html += '<tr id="map' + mapID + '">';
        }

        html += '<td class="one">';
        if(map['hasImage']) {
            html += '<img src="/api/getMapImage?mapID=' + mapID + '" alt="' + map['imageName'] + '" />';
        } else {
            html += 'No image';
        }
        html += '</td>';

        html += '<td class="two">';
        html += '<div class="title">' + map['title'] + '</div>';
        html += '<table class="udkAndAuthor floatLeftRight"><tr><td><div class="udkFilename">';
        html += map['name'].substr(0, map['name'].length - 4);
        html += '<span>';
        html += map['name'].substr(map['name'].length - 4, 4);
        html += '</span>';
        html += '</div></td><td><div class="authorName">';
        html += '<span>Created by </span>';
        html += map['authorName'];
        html += '</div></td></tr></table>';
        html += '<div class="description">' + coalesce(map['description'], "No description").replace(/\n/g, "<br />") + '</div>';
        html += '</td>';

        html += '<td class="three">';
        html += '<table class="floatLeftRight"><tr><td>';
        html += '<button class="loadMapButton" type="button" onclick="loadMapButtonClick(' + mapIDStr + ')">';
        html += thisMapIsLoaded ? 'Unload Map' : 'Load Map';
        html += '</button>';
        html += '</td><td>';
        // noinspection HtmlUnknownTarget
        html += '<img class="favorite' + (map['isFavorite'] ? ' isFavorite' : '') + '" alt="Mark as favorite" src="/img/star.png" onclick="favoriteClick(' + mapIDStr + ')" />';
        html += '</td></tr></table>';

        html += '<span class="lastLoaded">Last loaded: <wbr />';
        html += map['lastLoadedTimestamp'] <= 0 ? 'never' : lastPlayedFormatter.format(new Date(map['lastLoadedTimestamp']));
        html += '</span>';

        html += '<br />';

        html += '<button class="refreshMapMetadataButton" type="button" onclick="refreshMapMetadata(' + mapIDStr + ')">Refresh metadata</button>';
        html += '<br />';
        html += '<a href="https://steamcommunity.com/sharedfiles/filedetails/?id=' + mapID + '" target="_blank" rel="noreferrer">Visit workshop page</a>';

        html += '<div class="mapIDAndSize" style="font-size: 14px; margin-top: 48px;">';
        html += 'Map ID: ' + mapID;
        html += '<br />';
        html += 'Map Size: ' + (parseFloat(map['mapSize']) / 1048576).toFixed(1) + ' MiB';
        html += '</div>';

        html += '</td>';

        html += '</tr>';
    }

    html += '</table>';

    $('#mapTableContainer').html(html);
}

function favoriteClick(mapID) {
    let map = getMapDataById(mapID);
    if(!map) {
        return;
    }

    let isFavorite = map['isFavorite'] = !map['isFavorite'];

    let $favorite = $('#map' + mapID + ' .favorite');
    if(isFavorite) {
        $favorite.addClass('isFavorite');
    } else {
        $favorite.removeClass('isFavorite');
    }

    makeRequest('api/setFavorite?mapID=' + mapID + '&isFavorite=' + (isFavorite ? '1' : '0'));
}

function loadMap(mapID) {
    if(mapID === loadedMapID) {
        return;
    }
    let map = getMapDataById(mapID);
    if(!map) {
        return;
    }

    let $newMapButton = $('#map' + mapID + ' .loadMapButton');
    $newMapButton.attr('disabled', '').html('Loading...');

    makeRequest('api/loadMap?mapID=' + mapID, null, function() {
        loadedMapID = mapID;
        map['lastLoadedTimestamp'] = Date.now();

        refreshMapView();
        scrollMapIntoView(mapID, 'ifScrolled');
    });
}

function unloadMap(callback) {
    if(loadedMapID === '0') {
        if(callback) {
            callback();
        }
        return;
    }

    makeRequest('api/unloadMap', null, function() {
        $('#map' + loadedMapID).removeClass('loaded');
        $('#map' + loadedMapID + ' .loadMapButton').html('Load Map');

        loadedMapID = '0';

        if(callback) {
            callback();
        }
    });
}

function loadMapButtonClick(mapID) {
    if(loadedMapID === mapID) {
        unloadMap();
    } else {
        loadMap(mapID);
    }
}

function refreshMapMetadata(mapID) {
    let $button = $('#map' + mapID + ' button.refreshMapMetadataButton');
    $button.attr('disabled', '').html('Refreshing...');

    makeRequest('api/refreshMapMetadata?mapID=' + mapID, null, function() {
        $button.html('Done.');
        setTimeout(loadMaps, 2000);
    }, function() {
        $button.html('Error!');
        setTimeout(function() {
            $button.attr('disabled', null).html('Refresh metadata');
        }, 2000);
    }, 5000);
}

function scrollMapIntoView(mapID, highlight) {
    let $tr = $('#map' + mapID);
    if($tr.length === 0) {
        return;
    }

    let tr = $tr.get(0);

    const windowHeight = $(window).height();
    const currentScrollPos = $(document).scrollTop();
    const trStart = $tr.offset().top;
    const trHeight = $tr.height();
    const trEnd = trStart + trHeight;

    const visibleAreaStart = currentScrollPos + 100;
    const visibleAreaEnd = currentScrollPos + windowHeight - 100;

    const needsScrolling = trStart < visibleAreaStart || visibleAreaEnd < trEnd;

    if(needsScrolling) {
        tr.scrollIntoView({behavior: "smooth", block: "center"});
    }

    if(highlight === 'always' || highlight === 'ifScrolled' && needsScrolling) {
        setTimeout(function() {
            $tr.css('background-color', '#888');
            setTimeout(function() {
                $tr.css('transition', '1s ease-in background-color');
                $tr.css('background-color', '');
            }, 50);
            setTimeout(function() {
                $tr.css('transition', '');
            }, 1050);
        }, needsScrolling ? 600 : 50);
    }
}