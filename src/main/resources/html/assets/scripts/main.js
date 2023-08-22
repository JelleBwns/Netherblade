let socket;

function handle(i, e) {
    let json = new Object();
    json.type = i;
    json.x = e.clientX;
    json.y = e.clientY;
    json.moveX = e.movementX;
    json.moveY = e.movementY;
    json.navbar = e.target.matches('.navbar');
    socket.send(JSON.stringify(json));
}

window.onload = function () {
    addEventListener("mousemove", e => {
        handle(1, e)
    });
    addEventListener("mousedown", e => {
        handle(2, e)
    });
    addEventListener("click", e => {
        handle(3, e)
    });
    fetch('http://localhost:35199/v1/client/available')
        .then((response) => response.json())
        .then((data) => {
            const dropdown = document.getElementById('regions');
            data['regions'].sort();
            data['regions'].forEach((region) => {
                let option = document.createElement("option");
                if (region === "EUW") option.selected = true;
                option.innerHTML = region;
                option.value = region;
                dropdown.appendChild(option);
            });
            // sort dropdown and make TEST always last
            let options = Array.from(dropdown.options);
            options.sort((a, b) => {
                if (a.value === "TEST") return 1;
                if (b.value === "TEST") return -1;
                return a.value.localeCompare(b.value);
            });
            dropdown.innerHTML = "";
            for (const option of options) {
                dropdown.appendChild(option);
            }
        })
        .catch((error) => {
            console.error('Error:', error);
        });
    connect("ws://127.0.0.1:8887");
    document.addEventListener("click", e => {
        if (e.target.matches('.fa-chevron-down') || e.target.matches('.fa-chevron-up')) {
            flip(e.target.parentNode);
        }
        if (e.target.matches('.expand')) {
            flip(e.target)
        }
    });
    var search = document.getElementById('search');
    search.addEventListener('keyup', filter);

    var methodsFilter = document.getElementById('methodsFilter');
    methodsFilter.addEventListener('change', filter);
}

function call(url) {
    fetch(url)
        .catch((error) => {
            console.error('Error:', error);
        });

}

function dispose() {
    call('http://localhost:35199/v1/config/close');
}

function maximize() {
    call('http://localhost:35199/v1/config/maximize');
}

function minimize() {
    call('http://localhost:35199/v1/config/minimize');
}

function wiki() {
    call('http://localhost:35199/v1/config/wiki');
}

function config() {
    call('http://localhost:35199/v1/config/load');
}

function wipe() {
    document.getElementById('display').innerHTML = "";
}

function filter() {
    const selected = document.getElementById('methodsFilter').value.toLowerCase();
    const query = document.getElementById('search').value.toLowerCase();
    const display = document.getElementById('display');
    const children = Array.from(display.childNodes);

    children.forEach((child) => {
        if (child.outerHTML === undefined) return;
        const method = child.querySelector('.method').innerHTML.toLowerCase();
        const source = child.outerHTML.toLowerCase();
        const shouldShow = source.includes(query) && (selected === 'all' || method === selected);
        toggleHiddenClass(child, shouldShow);
    });
}

function toggleHiddenClass(element, shouldShow) {
    if (shouldShow) {
        if (element.classList.contains('hidden')) {
            element.classList.remove('hidden');
        }
    } else {
        if (!element.classList.contains('hidden')) {
            element.classList.add('hidden');
        }
    }
}

function flip(e) {
    for (const child of e.children) {
        child.classList.toggle('hidden');
    }
    const mainhint = e.parentNode; //main-hint
    const hint = mainhint.parentNode; //hint
    const bonushint = hint.lastElementChild;
    const visible = bonushint.classList.contains("hidden");
    if (visible) {
        mainhint.style = "border-radius: 8px 8px 0 0";
    } else {
        mainhint.style = "";
    }
    bonushint.classList.toggle('hidden');
}

function launch() {
    const region = document.getElementById('regions').value;
    fetch('http://localhost:35199/v1/client/launch/' + region)
        .then((response) => response.text())
        .then((text) => {
            console.log(text);
        })
        .catch((error) => {
            console.error('Error:', error);
        });
}

function connect(host) {
    socket = new WebSocket(host);
    socket.onopen = function (msg) {
        console.log("Connected to " + host);
    };
    socket.onmessage = function (msg) {
        const json = JSON.parse(msg.data);
        if (json['type'] === 'http') {
            appendHTML(json);
        } else if (json['type'] === 'rtmp') {
            appendRTMP(json);
        } else if (json['type'] === 'xmpp') {
            appendXMPP(json);
        } else if (json['type'] === 'rms') {
            appendRMS(json);
        } else {
            console.log("unknown type: " + json['type']);
        }
        filter();
        //methodsFilterHandler();
    };
    socket.onclose = function (msg) {
        console.log("disconnected from " + host);
    };
}

function appendXMPP(json) {
    appendSpecial('XMPP', json);
}

function appendRTMP(json) {
    appendSpecial('RTMP', json);
}

function appendRMS(json) {
    appendSpecial('RMS', json);
}

function appendSpecial(name, json) {
    const request = document.createElement("div");
    request.className = "request";
    let ingoing = json.hasOwnProperty("in");
    let value = json[ingoing ? 'in' : 'out'];
    request.appendChild(hintSpecial(name, ingoing, value));
    const display = document.getElementById('display');
    display.insertBefore(request, display.firstChild);
}

function hintSpecial(name, ingoing, value) {
    const hint = document.createElement("div");
    hint.className = "hint";
    const main = document.createElement("div");
    main.className = "main-hint";
    const method = document.createElement("div");
    method.className = "method";
    method.innerHTML = name;
    main.appendChild(method);
    const uri = document.createElement("div");
    uri.className = "uri";
    switch (name) {
        case "RMS":
            let resource = value.hasOwnProperty('payload') && value['payload'].hasOwnProperty("resource");
            uri.innerHTML = resource ? value['subject'] + " " + value['payload']['resource'] : value['subject'];
            break;
        case "XMPP":
            let regex = /id=\"(.*?)\"/g;
            let match = regex.exec(value);
            if (match) {
                uri.innerHTML = match[1];
            } else {
                regex = /^<\/?(.*?)[. >]/g;
                match = regex.exec(value);
                if (match) {
                    uri.innerHTML = match[1];
                } else {
                    uri.innerHTML = "heartbeat";
                }
            }
            break;
        case "RTMP":
            switch (value['result']) {
                case "receive":
                    let response = value['data']['flex.messaging.messages.AsyncMessage']['body']['com.riotgames.platform.serviceproxy.dispatch.LcdsServiceProxyResponse']
                    uri.innerHTML = "receive: " + response['serviceName'] + " " + response['methodName'] + " " + response['messageId'];
                    break;
                case "_result":
                    uri.innerHTML = "result id: " + value['invokeId'];
                    break;
                default:
                    let data = value['data'];
                    uri.innerHTML = "invoke id: " + value['invokeId'] + " " + data['destination'] + " " + data['operation'];
                    break;

            }
            break;
    }
    main.appendChild(uri);
    const code = document.createElement("div");
    code.className = "code";
    code.innerHTML = ingoing ? 'IN' : 'OUT';
    main.appendChild(code);
    main.appendChild(expand())
    hint.appendChild(main);
    hint.appendChild(specialBonus(value))
    return hint;
}

function specialBonus(value) {
    const bonus = document.createElement("div");
    bonus.className = "bonus-hint hidden";
    bonus.appendChild(content(value));
    return bonus;
}

function content(value) {
    const center = document.createElement("div");
    center.className = "data";
    const body = document.createElement("div");
    body.className = "request-group";
    body.appendChild(header("Content", "cp-body"));
    const text = document.createElement("pre");
    text.className = "request-value";
    switch (encoding(value)) {
        case "json":
            text.textContent = JSON.stringify(value, null, 2);
            break;
        case "xml":
            text.textContent = formatXml(value);
            break
        default:
            text.textContent = value;
            break
    }
    text.onclick = function () {
        if (text.textContent.trim().length === 0 || text.textContent === "Empty Body") {
            return;
        }
        const selection = window.getSelection();
        const range = document.createRange();
        range.selectNodeContents(text);
        selection.removeAllRanges();
        selection.addRange(range);
        document.execCommand("copy");
        selection.removeAllRanges();
    }
    body.appendChild(text);
    center.appendChild(body);
    return center;
}

function appendHTML(json) {
    const request = document.createElement("div");
    request.className = "request";
    request.appendChild(hint(json['request'], json['received']));
    const display = document.getElementById('display');
    display.insertBefore(request, display.firstChild);
}

function hint(request, response) {
    const hint = document.createElement("div");
    hint.className = "hint";
    const main = document.createElement("div");
    main.className = "main-hint";
    const method = document.createElement("div");
    method.className = "method";
    method.innerHTML = request['method'];
    main.appendChild(method);
    const uri = document.createElement("div");
    uri.className = "uri";
    uri.innerHTML = request['uri'];
    main.appendChild(uri);
    const code = document.createElement("div");
    code.className = "code";
    code.innerHTML = response['code'];
    main.appendChild(code);
    main.appendChild(expand())
    hint.appendChild(main);
    hint.appendChild(bonus(request, response))
    return hint;
}

function bonus(request, response) {
    const bonus = document.createElement("div");
    bonus.className = "bonus-hint hidden";
    bonus.appendChild(left(request));
    bonus.appendChild(right(response));
    return bonus;
}

function header(title, type) {
    const header = document.createElement("div");
    header.classList = "noselect group-name " + type;
    header.innerHTML = title;
    return header;
}

function JWTHandler(target, body) {
    const jwtRegex = /eyJ[A-Za-z0-9-_=]+\.[A-Za-z0-9-_=]+\.?[A-Za-z0-9-_.+\/=]*/gm;
    let match;

    while ((match = jwtRegex.exec(body)) !== null) {
        if (match.index === jwtRegex.lastIndex) {
            jwtRegex.lastIndex++;
        }

        match.forEach((match, groupIndex) => {
            appendJWTDecodeButton(target, match);
        });
    }
}

function appendJWTDecodeButton(parent, token) {
    const jwtButton = document.createElement("button");
    jwtButton.className = "jwt-button";
    jwtButton.innerHTML = "Decode JWT";

    jwtButton.onclick = function () {
        const isToggled = jwtButton.classList.contains("toggled");

        if (isToggled) {
            jwtButton.classList.remove("toggled");
            const decodedJWTString = jwtButton.getAttribute("decodedJwt");
            const originalJWT = jwtButton.getAttribute("originalJwt");

            replaceTextContent(parent, decodedJWTString, originalJWT)
            replaceTextContent(jwtButton, "Original JWT", "Decode JWT")
        } else {
            const decodedJWT = decodeJWT(token);
            const decodedJWTString = JSON.stringify(decodedJWT, null, 2);

            jwtButton.setAttribute("originalJwt", token);
            jwtButton.setAttribute("decodedJwt", decodedJWTString);
            replaceTextContent(parent, token, decodedJWTString);
            replaceTextContent(jwtButton, "Decode JWT", "Original JWT")
            jwtButton.classList.add("toggled");
        }
    };
    parent.appendChild(jwtButton);
}

function replaceTextContent(element, searchText, replacementText) {
    const nodeIterator = document.createNodeIterator(element, NodeFilter.SHOW_TEXT);
    let currentNode;

    while ((currentNode = nodeIterator.nextNode())) {
        if (currentNode.nodeValue.includes(searchText)) {
            const newNode = document.createTextNode(currentNode.nodeValue.replace(searchText, replacementText));
            currentNode.parentNode.replaceChild(newNode, currentNode);
        }
    }
}

function decodeJWT(token) {
    var base64Url = token.split('.')[1];
    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    var jsonPayload = decodeURIComponent(window.atob(base64).split('').map(function (c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));

    return JSON.parse(jsonPayload);
}

function left(request) {
    const left = document.createElement("div");
    left.className = "requestdata";
    left.appendChild(header("REQUEST", "group-main group-left"));
    const parameters = document.createElement("div");
    parameters.className = "request-group";
    parameters.appendChild(header("QUERY", "cp-query"));
    const params = request['query'];
    for (var i = 0; i < params.length; i++) {
        let key = params[i]['k'];
        let value = params[i]['v'];
        const param = document.createElement("div");
        param.className = "request-value";
        param.innerHTML = key + ": " + value;
        parameters.appendChild(param);
    }
    left.appendChild(parameters);
    const headers = document.createElement("div");
    headers.className = "request-group";
    headers.appendChild(header("HEADERS", "cp-headers"));
    const fields = request['headers'];
    for (var i = 0; i < fields.length; i++) {
        let key = fields[i]['k'];
        let value = fields[i]['v'];
        const header = document.createElement("div");
        header.className = "request-value";
        header.innerHTML = key + ": " + value;

        JWTHandler(header, value)

        headers.appendChild(header);
    }
    left.appendChild(headers);
    const body = document.createElement("div");
    body.className = "request-group";
    body.appendChild(header("BODY", "cp-body"));
    const text = document.createElement("pre");
    text.className = "request-value";
    let value = request['body'];
    switch (encoding(value)) {
        case "json":
            text.textContent = JSON.stringify(JSON.parse(value), null, 2);
            break;
        case "xml":
            text.textContent = formatXml(value);
            break
        default:
            text.textContent = value;
            break
    }

    if (value.length === 0) {
        text.textContent = "Empty Body";
    }
    JWTHandler(text, value)

    body.appendChild(text);
    left.appendChild(body);
    return left;
}

function right(response) {
    const right = document.createElement("div");
    right.className = "responsedata";
    right.appendChild(header("RESPONSE", "group-main group-right"));
    const headers = document.createElement("div");
    headers.className = "request-group";
    headers.appendChild(header("HEADERS", "cp-headers"));
    const fields = response['headers'];
    for (var i = 0; i < fields.length; i++) {
        let key = fields[i]['k'];
        let value = fields[i]['v'];
        const header = document.createElement("div");
        header.className = "request-value";

        JWTHandler(header, value)

        header.innerHTML = key + ": " + value;
        headers.appendChild(header);
    }
    right.appendChild(headers);
    const body = document.createElement("div");
    body.className = "request-group";
    body.appendChild(header("BODY", "cp-body"));
    const text = document.createElement("pre");
    text.className = "request-value";
    let value = response['body'];
    switch (encoding(value)) {
        case "json":
            text.textContent = JSON.stringify(JSON.parse(value), null, 2);
            break;
        case "xml":
            text.textContent = formatXml(value);
            break
        default:
            text.textContent = value;
            break
    }

    if (value.length === 0) {
        text.textContent = "Empty Body";
    }

    const copyButton = document.createElement("button");
    copyButton.textContent = "Copy Text";
    copyButton.className = "copy-button";
    copyButton.addEventListener("click", function() {
        copyToClipboard(value);
    });

    JWTHandler(text, value)

    body.appendChild(copyButton);
    body.appendChild(text);
    right.appendChild(body);
    return right;
}

// Function to copy text to clipboard
function copyToClipboard(text) {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand("copy");
    document.body.removeChild(textArea);
}


function expand() {
    const expand = document.createElement("div");
    expand.className = "expand";
    const down = document.createElement("i");
    down.classList = "fa-solid fa-chevron-down";
    expand.appendChild(down);
    const up = document.createElement("i");
    up.classList = "fa-solid fa-chevron-up hidden";
    expand.appendChild(up);
    return expand;
}

function encoding(o) {
    if (o.length === 0) return "plain";
    else if (JSON.stringify(o).charAt(0) === '{') return "json";
    else if (o.charAt(0) === '{') return "json";
    else if (o.charAt(0) === '<') return "xml";
    else return "plain";
}

function formatXml(xml) {
    var formatted = '';
    var reg = /(>)(<)(\/*)/g;
    xml = xml.toString().replace(reg, '$1\r\n$2$3');
    var pad = 0;
    var nodes = xml.split('\r\n');
    for (var n in nodes) {
        var node = nodes[n];
        var indent = 0;
        if (node.match(/.+<\/\w[^>]*>$/)) {
            indent = 0;
        } else if (node.match(/^<\/\w/)) {
            if (pad !== 0) {
                pad -= 1;
            }
        } else if (node.match(/^<\w[^>]*[^\/]>.*$/)) {
            indent = 1;
        } else {
            indent = 0;
        }

        var padding = '';
        for (var i = 0; i < pad; i++) {
            padding += '  ';
        }

        formatted += padding + node + '\r\n';
        pad += indent;
    }
    return formatted;
}
