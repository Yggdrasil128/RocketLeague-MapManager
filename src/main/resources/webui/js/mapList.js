let maps = [];
let loadedMapID = null;
let lastPlayedFormatter = new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
});
let mapComparator = null;
let mapComparatorOptions = null;

function getMapDataById(mapID) {
    for(let map of maps) {
        if(map['id'] === mapID) {
            return map;
        }
    }
    return null;
}

function loadMaps() {
    makeRequest('api/getMaps', null, null, loadMapsCallback);
}

function loadMapsCallback(data) {
    maps = JSON.parse(data);
    $('.mapListHeader h1').html('Map List (' + maps.length + ')');

    makeRequest('api/getLoadedMapID', null, null, function(data) {
        loadedMapID = data === 'null' ? null : data;
        sortMaps();
        refreshMapView();
        filterMaps();
    });
}

function sortMaps() {
    maps.sort(mapComparator);
}

function refreshMapView() {
    if(maps.length === 0) {
        $('#mapTableContainer').html('Map list is empty.');
        return;
    }

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

    let $favorite = $('#map-' + mapID + ' .favorite');
    if(isFavorite) {
        $favorite.addClass('isFavorite');
    } else {
        $favorite.removeClass('isFavorite');
    }

    makeRequest('api/setFavorite', {
        mapID: mapID,
        isFavorite: (isFavorite ? '1' : '0')
    });

    if($('#mapSorting_favoritesAtTop').get(0).checked) {
        setTimeout(function() {
            sortMaps();
            refreshMapView();
            filterMaps();
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

    let $newMapButton = $('#map-' + mapID + ' .loadMapButton');
    $newMapButton.attr('disabled', '').html('Loading...');

    makeRequest('api/loadMap', {mapID: mapID}, null, function() {
        loadedMapID = mapID;
        map['lastLoadedTimestamp'] = Date.now();

        sortMaps();
        refreshMapView();
        filterMaps();
        scrollMapIntoView(mapID, 'ifScrolled');
    });
}

function unloadMap(callback) {
    if(loadedMapID === null) {
        if(callback) {
            callback();
        }
        return;
    }

    makeRequest('api/unloadMap', null, null, function() {
        $('#map-' + loadedMapID).removeClass('loaded');
        $('#map-' + loadedMapID + ' .loadMapButton').html('Load Map');

        let oldLoadedMapID = loadedMapID;
        loadedMapID = '0';

        sortMaps();
        refreshMapView();
        filterMaps();
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
    let $button = $('#map-' + mapID + ' button.refreshMapMetadataButton');
    $button.attr('disabled', '').html('Refreshing...');

    makeRequest('api/refreshMapMetadata', {mapID: mapID}, null, function() {
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
    let $map = $('#map-' + mapID);
    if($map.length === 0) {
        return;
    }

    let element = $map.get(0);

    const windowHeight = $(window).height();
    const currentScrollPos = $(document).scrollTop();
    const elementStart = $map.offset().top;
    const elementHeight = $map.height();
    const elementEnd = elementStart + elementHeight;

    const visibleAreaStart = currentScrollPos + 100;
    const visibleAreaEnd = currentScrollPos + windowHeight - 100;

    const needsScrolling = elementStart < visibleAreaStart || visibleAreaEnd < elementEnd;

    if(needsScrolling) {
        element.scrollIntoView({behavior: "smooth", block: "center"});
    }

    if(highlight === 'always' || highlight === 'ifScrolled' && needsScrolling) {
        setTimeout(function() {
            $map.css('background-color', '#888');
            setTimeout(function() {
                $map.css('transition', '1s ease-in background-color');
                $map.css('background-color', '');
            }, 50);
            setTimeout(function() {
                $map.css('transition', '');
            }, 1050);
        }, needsScrolling ? 600 : 50);
    }
}

function mapComparator_title(mapA, mapB) {
    return mapA['title'].localeCompare(mapB['title']);
}

function mapComparator_lastLoaded(mapA, mapB) {
    let timestampA = mapA['lastLoadedTimestamp'] <= 0 ? mapA['addedTimestamp'] : mapA['lastLoadedTimestamp'];
    let timestampB = mapB['lastLoadedTimestamp'] <= 0 ? mapB['addedTimestamp'] : mapB['lastLoadedTimestamp'];
    return timestampB - timestampA;
}

function mapComparator_mapSize(mapA, mapB) {
    return parseFloat(mapB['mapSize']) - parseFloat(mapA['mapSize']);
}

function mapComparator_authorName(mapA, mapB) {
    return mapA['authorName'].localeCompare(mapB['authorName']);
}

const mapComparators = [
    null,
    mapComparator_title,
    mapComparator_lastLoaded,
    mapComparator_mapSize,
    null,
    mapComparator_authorName
];

function filterMaps() {
    let s = $('#mapSearch').val().toLowerCase();

    let odd = true;
    for(const map of maps) {
        let mapID = map['id'];
        let mapElement = $('#map-' + mapID);
        if(map['title'].toLowerCase().includes(s)) {
            mapElement.removeClass("hidden");
            if(odd) {
                mapElement.addClass("odd");
            } else {
                mapElement.removeClass("odd");
            }
            odd = !odd;
        } else {
            mapElement.addClass("hidden");
        }
    }
}

function loadMapListSettingsFromConfig() {
    $('#mapLayoutSelect').get(0).value = config['mayLayout'];
    $('#mapSortingSelect').get(0).value = config['mapSorting'];
    $('#mapSorting_loadedMapAtTop').get(0).checked = config['showLoadedMapAtTop'];
    $('#mapSorting_favoritesAtTop').get(0).checked = config['showFavoritesAtTop'];
    $('#mapSearchFocus').get(0).checked = config['focusSearchOnHotkey'];
}

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
    filterMaps();

    // the mapComparatorOptions object has already been updated by updateMapComparator
    makeRequest('api/patchConfig', null, JSON.stringify(mapComparatorOptions));
}

function onUpdateLayoutOption() {
    let i = refreshMapView();
    filterMaps();

    makeRequest('api/patchConfig', null, JSON.stringify({mapLayout: i}));
}

function onUpdateMapSearchFocus() {
    makeRequest('api/patchConfig', null, JSON.stringify({focusSearchOnHotkey: $('#mapSearchFocus').get(0).checked}));
}

function refreshMapView_compactList() {
    let html = '<table class="maps compactList">';

    for(const map of maps) {
        let mapID = map['id'];
        let mapIDStr = "'" + mapID + "'";
        let thisMapIsLoaded = loadedMapID === mapID;

        if(thisMapIsLoaded) {
            html += '<tr id="map-' + mapID + '" class="map loaded">';
        } else {
            html += '<tr id="map-' + mapID + '" class="map">';
        }

        html += '<td class="one">';
        if(map['hasImage']) {
            html += '<img src="/api/getMapImage?mapID=' + mapID + '&mtime=' + map['imageMTime'] + '" alt="' + map['title'] + '" />';
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
        html += '<div class="lastLoaded">';
        if(map['lastLoadedTimestamp'] <= 0) {
            html += 'Added: ';
            html += lastPlayedFormatter.format(new Date(map['addedTimestamp']));
        } else {
            html += 'Last loaded: ';
            html += lastPlayedFormatter.format(new Date(map['lastLoadedTimestamp']));
        }
        html += '</div>';
        if(map['url']) {
            html += '<a href="' + map['url'] + '" target="_blank" rel="noreferrer">';
            if(mapID.startsWith('S-')) {
                html += 'Visit Steam workshop page';
            } else if(mapID.startsWith('L-')) {
                html += 'Visit lethamyr.com page';
            } else {
                html += 'Visit page';
            }
            html += '</a>';
        }
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
            html += '<tr id="map-' + mapID + '" class="map loaded">';
        } else {
            html += '<tr id="map-' + mapID + '" class="map">';
        }

        html += '<td class="one">';
        if(map['hasImage']) {
            let imgSrc = '/api/getMapImage?mapID=' + mapID + '&mtime=' + map['imageMTime'];
            html += '<img src="' + imgSrc + '" alt="' + map['title'] + '" />';
        } else {
            html += 'No image';
        }
        html += '</td>';

        html += '<td class="two">';
        html += '<div class="title">' + map['title'] + '</div>';
        html += '<table class="udkAndAuthor floatLeftRight"><tr><td><div class="udkFilename">';
        html += map['udkName'].substr(0, map['udkName'].length - 4);
        html += '<span>';
        html += map['udkName'].substr(map['udkName'].length - 4, 4);
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

        html += '<span class="lastLoaded">';
        if(map['lastLoadedTimestamp'] <= 0) {
            html += 'Added: <wbr />';
            html += lastPlayedFormatter.format(new Date(map['addedTimestamp']));
        } else {
            html += 'Last loaded: <wbr />';
            html += lastPlayedFormatter.format(new Date(map['lastLoadedTimestamp']));
        }
        html += '</span>';

        html += '<br />';

        html += '<button class="refreshMapMetadataButton" type="button" onclick="refreshMapMetadata(' + mapIDStr + ')">Refresh metadata</button>';
        html += '<br />';
        if(map['url']) {
            html += '<a href="' + map['url'] + '" target="_blank" rel="noreferrer">';
            if(mapID.startsWith('S-')) {
                html += 'Visit Steam workshop page';
            } else if(mapID.startsWith('L-')) {
                html += 'Visit lethamyr.com page';
            } else {
                html += 'Visit page';
            }
            html += '</a>';
        }

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
    let html = '<div class="maps gridView">';

    let count = 0;
    for(const map of maps) {
        let mapID = map['id'];
        let mapIDStr = "'" + mapID + "'";
        let thisMapIsLoaded = loadedMapID === mapID;

        if(thisMapIsLoaded) {
            html += '<div id="map-' + mapID + '" class="map loaded">';
        } else {
            html += '<div id="map-' + mapID + '" class="map">';
        }

        html += '<div class="one">';
        if(map['hasImage']) {
            html += '<img src="/api/getMapImage?mapID=' + mapID + '&mtime=' + map['imageMTime'] + '" alt="' + map['title'] + '" />';
        } else {
            html += '<div>No image</div>';
        }
        html += '</div>';

        html += '<table class="floatLeftRight two"><tr><td class="title">';
        html += map['title'];
        html += '</td><td>';
        html += '<img class="favorite' + (map['isFavorite'] ? ' isFavorite' : '') + '" alt="Mark as favorite" src="/img/star.png" onclick="favoriteClick(' + mapIDStr + ')" />';
        html += '</td></tr></table>';

        html += '<table class="floatLeftRight three"><tr><td>';
        html += '<div class="lastLoaded">';
        if(map['lastLoadedTimestamp'] <= 0) {
            html += 'Added: ';
            html += lastPlayedFormatter.format(new Date(map['addedTimestamp']));
        } else {
            html += 'Last loaded: ';
            html += lastPlayedFormatter.format(new Date(map['lastLoadedTimestamp']));
        }
        html += '</div>';
        if(map['url']) {
            html += '<a href="' + map['url'] + '" target="_blank" rel="noreferrer">';
            if(mapID.startsWith('S-')) {
                html += 'Visit Steam workshop page';
            } else if(mapID.startsWith('L-')) {
                html += 'Visit lethamyr.com page';
            } else {
                html += 'Visit page';
            }
            html += '</a>';
        }
        html += '</td><td>';
        html += '<button class="loadMapButton" type="button" onclick="loadMapButtonClick(' + mapIDStr + ')">';
        html += thisMapIsLoaded ? 'Unload Map' : 'Load Map';
        html += '</button>';
        html += '</td></tr></table>';

        html += '</div>';
        count++;
    }

    html += '<div class="filler"></div>';
    html += '<div class="filler"></div>';

    html += '</div>';

    $('#mapTableContainer').html(html);
}
