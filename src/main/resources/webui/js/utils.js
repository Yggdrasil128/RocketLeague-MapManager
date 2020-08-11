let rlmmVersion = null;
const browserTabID = getRandomString(8);

$(function() {
    // clock
    setInterval(function() {
        const now = new Date();

        let i = now.getHours();
        let s = (i >= 10 ? i : '0' + i) + ":";
        i = now.getMinutes();
        s += (i >= 10 ? i : '0' + i) + ":";
        i = now.getSeconds();
        s += i >= 10 ? i : '0' + i;

        $('#localTime').html(s);
    }, 1000);

    // fetch version
    makeRequest('/api/getVersion', null, null, function(version) {
        rlmmVersion = version;
        $('#versionDiv span').html(version);
    });
});

function makeRequest(url, params, body, successCallback, errorCallback, timeoutMillis) {
    let request = new XMLHttpRequest();
    request.onload = function() {
        if(200 <= this.status && this.status <= 299) {
            if(successCallback) {
                successCallback(this.responseText);
            }
        } else {
            if(errorCallback) {
                errorCallback();
            }
        }
    };
    url += '?btid=' + browserTabID;
    if(params) {
        for(let key in params) {
            if(params.hasOwnProperty(key)) {
                url += '&' + key + '=' + params[key];
            }
        }
    }
    request.onerror = errorCallback;
    request.ontimeout = errorCallback;
    request.open(body ? "POST" : "GET", url, true);
    request.timeout = timeoutMillis ? timeoutMillis : 1000;
    if(body) {
        request.send(body);
    } else {
        request.send();
    }
}

/**
 * @param a {*}
 * @param b {*}
 * @returns {*} the first argument if it is truthy, or else the second argument.
 */
function coalesce(a, b) {
    return a ? a : b;
}

function getRandomString(length) {
    let result = '';
// noinspection SpellCheckingInspection
    let characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let charactersLength = characters.length;
    for(let i = 0; i < length; i++) {
        result += characters.charAt(Math.floor(Math.random() * charactersLength));
    }
    return result;
}
