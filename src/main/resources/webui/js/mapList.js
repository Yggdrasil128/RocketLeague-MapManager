let maps = [];
let loadedMapID = '0';
let lastPlayedFormatter = new Intl.DateTimeFormat(undefined, {year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'});
let mapComparator = null;
let mapComparatorOptions = null;

$(function() {
    updateMapComparator();
});

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
    $('.mapListHeader h1').html('Map List (' + maps.length + ')');

    makeRequest('api/getLoadedMapID', null, function(data) {
        loadedMapID = data;
        sortMaps();
        refreshMapView();
    });
}

function sortMaps() {
    maps.sort(mapComparator);
}

function refreshMapView() {
    let i = parseInt($('#mapLayoutSelect').get(0).value);
    switch(i) {
        case 0:
            refreshMapView_compactList();
            break;
        case 1:
            refreshMapView_detailedList();
            break;
        case 2:
            refreshMapView_gridView();
            break;
    }
    return i;
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

    if($('#mapSorting_favoritesAtTop').get(0).checked) {
        setTimeout(function() {
            sortMaps();
            refreshMapView();
            scrollMapIntoView(mapID, 'ifScrolled');
        }, 1000);
    }
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

        sortMaps();
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

        let oldLoadedMapID = loadedMapID;
        loadedMapID = '0';

        sortMaps();
        refreshMapView();
        scrollMapIntoView(oldLoadedMapID, 'ifScrolled');

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

function mapComparator_title(mapA, mapB) {
    return mapA['title'].localeCompare(mapB['title']);
}

function mapComparator_lastLoaded(mapA, mapB) {
    return mapB['lastLoadedTimestamp'] - mapA['lastLoadedTimestamp'];
}

function mapComparator_mapSize(mapA, mapB) {
    return parseFloat(mapB['mapSize']) - parseFloat(mapA['mapSize']);
}

function mapComparator_mapID(mapA, mapB) {
    return parseFloat(mapA['id']) - parseFloat(mapB['id']);
}

const mapComparators = [
    null,
    mapComparator_title,
    mapComparator_lastLoaded,
    mapComparator_mapSize,
    mapComparator_mapID
];

function updateMapComparator() {
    mapComparatorOptions = {
        mapSorting: parseInt($('#mapSortingSelect').get(0).value),
        showLoadedMapAtTop: $('#mapSorting_loadedMapAtTop').get(0).checked,
        showFavoritesAtTop: $('#mapSorting_favoritesAtTop').get(0).checked,
    };

    const reversed = mapComparatorOptions.mapSorting < 0;

    let comparator1 = mapComparators[Math.abs( mapComparatorOptions.mapSorting)];

    let comparator2 = !reversed ? comparator1 : function(mapA, mapB) {
        return -comparator1(mapA, mapB);
    };

    let comparator3 = !mapComparatorOptions.showFavoritesAtTop ? comparator2 : function(mapA, mapB) {
        if(mapA['isFavorite'] ^ mapB['isFavorite']) {
            return mapA['isFavorite'] ? -1 : 1;
        }
        return comparator2(mapA, mapB);
    };

    mapComparator = !mapComparatorOptions.showLoadedMapAtTop ? comparator3 : function(mapA, mapB) {
        if(loadedMapID === mapA['id']) {
            return -1;
        }
        if(loadedMapID === mapB['id']) {
            return 1;
        }
        return comparator3(mapA, mapB);
    };
}

function onUpdateSortOptions() {
    updateMapComparator();
    sortMaps();
    refreshMapView();

    // the mapComparatorOptions object has already been updated by updateMapComparator
    makeRequest('api/patchConfig', JSON.stringify(mapComparatorOptions));
}

function onUpdateLayoutOption() {
    let i = refreshMapView();

    makeRequest('api/patchConfig', JSON.stringify({mapLayout: i}));
}

function refreshMapView_compactList() {
    let html = '<table class="maps compactList">';

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
        html += '</td>';

        html += '<td class="three">';
        html += '<div class="authorName">';
        // html += '<span>Created by </span>';
        html += map['authorName'];
        html += '</div>';
        html += '</td>';

        html += '<td class="four">';
        html += '<div class="lastLoaded">Last loaded: ';
        html += map['lastLoadedTimestamp'] <= 0 ? 'never' : lastPlayedFormatter.format(new Date(map['lastLoadedTimestamp']));
        html += '</div>';
        html += '<a href="https://steamcommunity.com/sharedfiles/filedetails/?id=' + mapID + '" target="_blank" rel="noreferrer">Visit workshop page</a>';
        html += '</td>';

        html += '<td class="five">';
        html += '<button class="loadMapButton" type="button" onclick="loadMapButtonClick(' + mapIDStr + ')">';
        html += thisMapIsLoaded ? 'Unload Map' : 'Load Map';
        html += '</button>';
        html += '</td>';

        html += '<td class="six">';
        html += '<img class="favorite' + (map['isFavorite'] ? ' isFavorite' : '') + '" alt="Mark as favorite" src="/img/star.png" onclick="favoriteClick(' + mapIDStr + ')" />';
        html += '</td>';

        html += '</tr>';
    }

    html += '</table>';

    $('#mapTableContainer').html(html);
}

function refreshMapView_detailedList() {
    let html = '<table class="maps detailedList">';

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

function refreshMapView_gridView() {
    $('#mapTableContainer').html('Grid View is not implemented yet.');
}