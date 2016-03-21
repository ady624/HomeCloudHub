/**
 *  Home Cloud Hub
 *
 *  Copyright 2016 Adrian Caramaliu
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Home Cloud Hub",
    namespace: "ady624",
    author: "Adrian Caramaliu",
    description: "Provides integration with AT&T Digital Life, MyQ and others",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

private getMyQAppId() {
    return 'Vj8pQggXLhLy0WHahglCD4N1nAkkXQtGYpq2HrHD7H1nvmbT55KqtN6RSF4ILB/i'
	return 'JVM/G9Nwih5BwKgNCjLxiFUQxQijAebyyg8QUHr7JOrP+tuPb8iHfRHKwTmDzHOu'
}

private getLocalServerURN() {
	return "urn:schemas-upnp-org:device:HomeCloudHubLocalServer:624"
}

preferences {
	page(name: "prefWelcome", title: "Welcome to Home Cloud Hub")
	page(name: "prefHCH", title: "Connect to Home Cloud Hub")
    page(name: "prefModulesPrepare", title: "Home Cloud Hub Modules")
    page(name: "prefModules", title: "Home Cloud Hub Modules")
	page(name: "prefATT", title: "AT&T Digital Life™ Integration")
	page(name: "prefATTConfirm", title: "AT&T Digital Life™ Integration")
	page(name: "prefMyQ", title: "MyQ™ Integration")
	page(name: "prefMyQConfirm", title: "MyQ™ Integration")
	page(name: "prefIFTTT", title: "IFTTT™ Integration")
	page(name: "prefIFTTTConfirm", title: "IFTTT™ Integration")
}


/***********************************************************************/
/*                        INSTALLATION UI PAGES                        */
/***********************************************************************/
def prefWelcome() {
	state.ihch = [security: [:]]    
    dynamicPage(name: "prefWelcome", title: "Welcome to Home Cloud Hub", uninstall: state.installed) {
        section("Connection Type") {
            paragraph "Welcome to Home Cloud Hub. Please select a server connection method to start"
        }
        section() {
            href(name: "href",
                 title: "Use the Home Cloud Hub service",
                 required: false,
                 params: [next: true, hchLocal: false],
                 page: "prefHCH",
                 description: "Select this if you have an account with www.homecloudhub.com",
                 state: !state.ihch.useLocalServer ? "complete" : null)
        }
        section() {
            href(name: "href",
                 title: "Use a local Home Cloud Hub server you installed",
                 required: false,
                 params: [next: true, hchLocal: true],
                 page: "prefHCH",
                 description: "Select this if you have already installed a local server in your network",
                 state: state.ihch.useLocalServer ? "complete" : null)
        }
    }
}

def prefHCH(params) {
	if (params.next) {
		state.ihch.useLocalServer = params.hchLocal
    }
    return dynamicPage(name: "prefHCH", title: "Connect to Home Cloud Hub", nextPage: "prefModulesPrepare") {
    	if (state.ihch.useLocalServer) {
            atomicState.hchLocalServerIp = null
            searchForLocalServer()
            def cnt = 50
            def hchLocalServerIp = null
            while (cnt--) {
            	pause(200)
                hchLocalServerIp = atomicState.hchLocalServerIp
                if (hchLocalServerIp) {
                	state.ihch.localServerIp = hchLocalServerIp
                	break
                }
            }
            section("Automatic configuration") {
				if (hchLocalServerIp) {
					href(name: "href",
                         title: "Use local server ${hchLocalServerIp}",
                         required: false,
                         params: [next: true, hchLocalServerIp: hchLocalServerIp],
                         page: "prefModulesPrepare",
                         description: "Select this to use this detected local server",
                         state: "complete")
	            } else {
                	paragraph "Could not identify any local servers"
                }
            }
            
        	section("Manual Configuration") {
	            input("hchLocalServerIp", "text", title: "Enter the IP of your local server", required: true, defaultValue: "")
	        }
        } else {
            section(title: "", hidden: true) {
                paragraph "Create a Home Cloud Hub account and enter your credentials below"
                href(name: "hrefNotRequired",
                     title: "Home Cloud Hub registration",
                     required: false,
                     style: "external",
                     url: "http://www.homecloudhub.com/",
                     description: "tap to create an account"
                    )
            }
            section("Login Credentials"){
                input("hchUsername", "email", title: "Username", description: "Your Home Cloud Hub login")
                input("hchPassword", "password", title: "Password", description: "Your password")
            }
		}
    }
}

def prefModulesPrepare(params) {
	if (params.hchLocalServerIp) {
    	state.ihch.localServerIp = params.hchLocalServerIp
    } else {
        state.ihch.localServerIp = settings.hchLocalServerIp
    }
    
    if (doHCHLogin()) {
	    //prefill states for the modules
    	doATTLogin(true, true)
    	doMyQLogin(true, true)
    	doIFTTTLogin(true, true)
		return prefModules()
	} else {
    	if (state.ihch.useLocalServer) {
			return dynamicPage(name: "prefModulesPrepare",  title: "Error connecting to Home Cloud Hub local server") {
				section(){
					paragraph "Sorry, your local server does not seem to respond at ${state.ihch.localServerIp}."
				}
	        }
        } else {
			return dynamicPage(name: "prefModulesPrepare",  title: "Error connecting to Home Cloud Hub") {
				section(){
					paragraph "Sorry, the credentials you provided for Home Cloud Hub are invalid. Please go back and try again."
				}
	        }
        }
    }
}

def prefModules() {
    return dynamicPage(name: "prefModules", title: "Add modules to Home Cloud Hub", install: state.ihch.useATT || state.ihch.useMyQ || state.ihch.useIFTTT) {
        section() {
            paragraph "Select and configure any of the modules below"
        }
        section() {
            href(name: "href",
                 title: "AT&T Digital Life™",
                 required: false,
                 page: "prefATT",
                 description: "Enable integration with AT&T Digital Life™",
                 state: state.ihch.useATT ? "complete" : null)
        }
        section() {
            href(name: "href",
                 title: "MyQ™",
                 required: false,
                 page: "prefMyQ",
                 description: "Enable integration with MyQ™",
                 state: state.ihch.useMyQ ? "complete" : null)
        }
        section() {
            href(name: "href",
                 title: "IFTTT™",
                 required: false,
                 page: "prefIFTTT",
                 description: "Enable integration with IFTTT™",
                 state: state.ihch.useIFTTT ? "complete" : null)
        }
    }
    
}

def prefATT() {
    if (doHCHLogin()) {
		return dynamicPage(name: "prefATT", title: "AT&T Digital Life™ Integration", nextPage:"prefATTConfirm") {
            section() {
                paragraph "Home Cloud Hub can optionally integrate your AT&T Digital Life™ alarm system into SmartThings."
                paragraph "NOTE: Since AT&T Digital Life™ does not support OAuth, your username and password are required for integration. They will be stored in your personal instance of the application on SmartThings' servers and are NEVER shared with anyone, not even with Home Cloud Hub. The credentials are used to generate a set of temporary tokens that are shared with Home Cloud Hub."
            }
            section("Login Credentials"){
                input("attUsername", "email", title: "Username", description: "Your AT&T Digital Life™ login", required: false)
                input("attPassword", "password", title: "Password", description: "Your password", required: false)
            }
            section("Permissions") {
				input("attControllable", "bool", title: "Control AT&T Digital Life", required: true, defaultValue: true)
            }
    	}
	} else {
		return dynamicPage(name: "prefATT",  title: "Error connecting to Home Cloud Hub", install:false, uninstall:false) {
			section(){
				paragraph "Sorry, the credentials you provided for Home Cloud Hub are invalid. Please go back and try again."
			}
        }
    }
}

def prefATTConfirm() {
    if (doATTLogin(true, true)) {
		return dynamicPage(name: "prefATTConfirm", title: "AT&T Digital Life™ Integration", nextPage:"prefModules") {
			section(){
				paragraph "Congratulations! You have successfully connected your AT&T Digital Life™ system."
			}
    	}
	} else {
		return dynamicPage(name: "prefATTConfirm",  title: "AT&T Digital Life™ Integration") {
			section(){
				paragraph "Sorry, the credentials you provided for AT&T Digital Life™ are invalid. Please go back and try again."
			}
        }
    }
}

def prefMyQ() {
    return dynamicPage(name: "prefMyQ", title: "MyQ™ Integration", nextPage:"prefMyQConfirm") {
        section() {
            paragraph "Home Cloud Hub can optionally integrate your MyQ™ garage door system into SmartThings."
            paragraph "NOTE: Since MyQ™ does not support OAuth, your username and password are required for integration. They will be stored in your personal instance of the application on SmartThings' servers and are NEVER shared with anyone, not even with Home Cloud Hub. The credentials are used to generate a temporary token that is shared with Home Cloud Hub."
        }
        section("Login Credentials"){
            input("myqUsername", "email", title: "Username", description: "Your MyQ™ login", required: false)
            input("myqPassword", "password", title: "Password", description: "Your password", required: false)
        }
        section("Permissions") {
            input("myqControllable", "bool", title: "Control MyQ", required: true, defaultValue: true)
        }
    }
}

def prefMyQConfirm() {
    if (doMyQLogin(true, true)) {
		return dynamicPage(name: "prefMyQConfirm", title: "MyQ™ Integration", nextPage:"prefModules") {
			section(){
				paragraph "Congratulations! You have successfully connected your MyQ™ system."
			}
    	}
	} else {
		return dynamicPage(name: "prefMyQConfirm",  title: "MyQ™ Integration") {
			section(){
				paragraph "Sorry, the credentials you provided for MyQ™ are invalid. Please go back and try again."
			}
        }
    }
}

def prefIFTTT() {
    return dynamicPage(name: "prefIFTTT", title: "IFTTT™ Integration", nextPage:"prefIFTTTConfirm") {
        section() {
            paragraph "Home Cloud Hub can optionally integrate with IFTTT™ (IF This Then That) via the Maker channel, triggering immediate events to IFTTT™. To enable IFTTT™, please login to your IFTTT™ account and connect the Maker channel. Youu will be provided with a key that needs to be entered below"
            href(name: "hrefNotRequired",
                 title: "IFTTT Maker channel",
                 required: false,
                 style: "external",
                 url: "https://www.ifttt.com/maker",
                 description: "tap to go to IFTTT™ and connect the Maker channel"
                )
        }
        section("IFTTT Maker key"){
            input("iftttKey", "email", title: "Key", description: "Your IFTTT Maker key", required: false)
        }
    }
}

def prefIFTTTConfirm() {
    if (doIFTTTLogin(true, true)) {
		return dynamicPage(name: "prefIFTTTConfirm", title: "IFTTT™ Integration", nextPage:"prefModules") {
			section(){
				paragraph "Congratulations! You have successfully connected your IFTTT™ system."
			}
    	}
	} else {
		return dynamicPage(name: "prefIFTTTConfirm",  title: "IFTTT™ Integration") {
			section(){
				paragraph "Sorry, the credentials you provided for IFTTT™ are invalid. Please go back and try again."
			}
        }
    }
}












/***********************************************************************/
/*                           LOGIN PROCEDURES                          */
/***********************************************************************/
/* Login to Home Cloud Hub                                             */
/***********************************************************************/
private doHCHLogin() {
	if (state.ihch.useLocalServer) {
        atomicState.hchPong = false

		log.trace "Pinging local server at " + state.ihch.localServerIp
        sendLocalServerCommand state.ihch.localServerIp, "ping", ""

		def cnt = 50
        def hchPong = false
        while (cnt--) {
            pause(200)
            hchPong = atomicState.hchPong
            if (hchPong) {
                return true
            }
        }
        return false
	} else {
        return httpGet('https://www.homecloudhub.com/endpoint/02666328-0063-0086-0069-076278844647/manager/smartthingsapp/login/' + settings.hchUsername.bytes.encodeBase64() + '/' + settings.hchPassword.bytes.encodeBase64()) { response ->
            if (response.status == 200) {
                if (response.data.result && (response.data.result == "success") && response.data.data && response.data.data.endpoint) {
                    state.ihch.endpoint = response.data.data.endpoint
                    state.ihch.connected = now()
                    if (!state.ihch.security) {
                        state.ihch.security = [:]
                    }
                    return true
                } else {
                    return false
                }
            } else {
                return false
            }
        }
	}
}

/***********************************************************************/
/* Login to AT&T Digital Life                                          */
/***********************************************************************/
private doATTLogin(installing, force) {
	def module_name = 'digitallife';
    //if cookies haven't expired and unless we need to force a login, we report all is pink
    if (!installing && !force && state.hch.security[module_name] && state.hch.security[module_name].connected && (state.hch.security[module_name].expires > now())) {
		log.info "Reusing previously login for AT&T Digital Life"
		return true;
    }
    //setup our security descriptor
    def hch = (installing ? state.ihch : state.hch)
    hch.useATT = false;
    hch.security[module_name] = [
    	'enabled': !!(settings.attUsername || settings.attPassword),
        'controllable': settings.attControllable,
        'connected': false
    ]
    //check if the AT&T Digital Life module is enabled
	if (hch.security[module_name].enabled) {
    	log.info "Logging in to AT&T Digital Life..."
        //perform the initial login, retrieve cookies
        return httpPost([
        	uri: "https://my-digitallife.att.com/tg_wam/login.do",
            headers: [
				'Referer': 'https://my-digitallife.att.com/dl/',
				'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8',
                'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36'
            ],
            body: "source=DLNA&targetURL=https://my-digitallife.att.com/dl/#/authenticate&loginURL=https://my-digitallife.att.com/dl/#/login&userid=${settings.attUsername}&password=${settings.attPassword}"
        ]) { response ->
        	//check response, sometimes they redirect, that's fine, we don't need to follow the redirect, we just need the cookies
			if ((response.status == 200) || (response.status == 302)) {
				def cookies = []
                def c = "";
                //the hard part, get the cookies
				response.getHeaders('Set-Cookie').each {
                    def cookie = it.value.split(';')[0]
					cookies.push(cookie)
                    c = c + cookie + '; '
				}
                //using the cookies, retrieve the auth tokens
                return httpPost([
		       		uri: "https://my-digitallife.att.com/penguin/api/authtokens",
                    headers: [
                        "Referer": "https://my-digitallife.att.com/dl",
                        "Content-Type": "application/x-www-form-urlencoded; charset=utf-8",
                        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36",
                        "DNT": "1",
                        "Cookie": c
                    ],
	                body: "domain=DL&appKey=TI_3198CF46D58D3AFD_001"
       			]) { response2 ->
                	//check response, continue if 200 OK
                	if (response2.status == 200) {
                        if (response2.data && response2.data.content && response2.data.content.gateways && response2.data.content.gateways.length) {
                        	//save the cookies and tokens into the security descriptor
                            //cookies expire in 13 minutes, we'll use 12 minutes as an expiry to ensure we don't refuse reconnection
                        	hch.security[module_name].key = response2.data.content.gateways[0].id
                            hch.security[module_name].authToken = response2.data.content.authToken
                            hch.security[module_name].requestToken = response2.data.content.requestToken
                            hch.security[module_name].cookies = cookies
                            hch.security[module_name].connected = now()
                            hch.security[module_name].expires = now() + 720000 //expires in 12 minutes
                            log.info "Successfully connected to AT&T Digital Life"
                            hch.useATT = true;
                            return true;
                        }
                    }
                    return false;
				}
            } else {
                return false
            }
        } 	
	} else {
    	return true;
    }
}

/***********************************************************************/
/* Login to MyQ                                                        */
/***********************************************************************/
def doMyQLogin(installing, force) {
	def module_name = 'myq';
    //if cookies haven't expired and unless we need to force a login, we report all is pink
    if (!installing && !force && state.hch.security[module_name] && state.hch.security[module_name].connected && (state.hch.security[module_name].expires > now())) {
		log.info "Reusing previously login for MyQ"
		return true;
    }
    //setup our security descriptor
    def hch = (installing ? state.ihch : state.hch)
    hch.useMyQ = false
    hch.security[module_name] = [
    	'enabled': !!(settings.myqUsername || settings.myqPassword),
        'controllable': settings.myqControllable,
        'connected': false
    ]
    if (hch.security[module_name].enabled) {
    	log.info "Logging in to MyQ..."
        //perform the login, retrieve token
        def myQAppId = getMyQAppId()
        return httpGet("https://myqexternal.myqdevice.com/Membership/ValidateUserWithCulture?appId=${myQAppId}&securityToken=null&username=${settings.myqUsername}&password=${settings.myqPassword}&culture=en") { response ->
			//check response, continue if 200 OK
       		if (response.status == 200) {
				if (response.data && response.data.SecurityToken) {
                    hch.security[module_name].securityToken = response.data.SecurityToken
                    hch.security[module_name].connected = now()
                	hch.security[module_name].expires = now() + 7200000 //expires in 12 hours
					log.info "Successfully connected to MyQ"
                    hch.useMyQ = true
                	return true;
                }
			}
            return false;
 		}
    } else {
		return true;
	}
}

/***********************************************************************/
/* Login to IFTTT                                                      */
/***********************************************************************/
def doIFTTTLogin(installing, force) {
    //setup our security descriptor
    def hch = (installing ? state.ihch : state.hch)
    hch.useIFTTT = false
    if (settings.iftttKey) {
    	//verify the key
        return httpGet("https://maker.ifttt.com/trigger/test/with/key/" + settings.iftttKey) { response ->
			if (response.status == 200) {
				if (response.data == "Congratulations! You've fired the test event")
				    hch.useIFTTT = true
                	return true;
			}
            return false;
 		}
    } else {
		return true;
	}
}












/***********************************************************************/
/*            INSTALL/UNINSTALL SUPPORTING PROCEDURES                  */
/***********************************************************************/
def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def uninstalled() {
	//call home and bid them fairwell
    if (!state.hch.useLocalServer) {
		httpGet('https://www.homecloudhub.com/endpoint/02666328-0063-0086-0069-076278844647/manager/smartthingsapp/disconnect/' + state.hch.endpoint.bytes.encodeBase64())
    }
}

def initialize() {
	if (!state.hch.useLocalServer) {
		if (!state.accessToken) {
	    	//make sure OAuth is enabled in the SmartApp Settings if the next line gives you an error
            try {
	        	createAccessToken()
            } catch(e) {
            	log.error "Could not create Access Token for app. Please go to App Settings and enable/create OAuth Client/Secret pair."
            }
		}
	}
    
    state.installed = true    
    //get the installing hch state
    state.hch = state.ihch

	//login to all services
   	doATTLogin(false, false)
   	doMyQLogin(false, false)
	
	if (state.hch.useLocalServer) {
    	//initialize the local server
		sendLocalServerCommand state.hch.localServerIp, "init", [
        	server: getHubLanEndpoint(),
            modules: state.hch.security
        ]
    } else {
		//call home and tell them where to find us
    	log.info "Endpoint is at " + apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}")
		httpGet('https://www.homecloudhub.com/endpoint/02666328-0063-0086-0069-076278844647/manager/smartthingsapp/connect/' + state.hch.endpoint.bytes.encodeBase64() + '/' + apiServerUrl("/api/token/${state.accessToken}/smartapps/installations/${app.id}").bytes.encodeBase64())
	}
    
	state.hch.usesATT = !!(settings.attUsername || settings.attPassword)
	state.hch.usesIFTTT = !!settings.iftttKey
    
    if (state.hch.usesATT && settings.attControllable) {
    	/* subscribe to SmartThings Home Monitor to allow sync with AT&T Digital Life status */
		//subscribe(location, "alarmSystemStatus", shmHandler)
        
        /* subscribe to mode changes to allow sync with AT&T Digital Life */
        subscribe(location, modeChangeHandler)
        if (state.hch.useLocalServer) {
        	//listen to LAN incoming messages
        	subscribe(location, null, lanEventHandler, [filterEvents:false])
        }
   }
}


private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}






/***********************************************************************/
/*                    SMARTTHINGS EVENT HANDLERS                       */
/***********************************************************************/
def shmHandler(evt) {
	def shmState = location.currentState("alarmSystemStatus")?.value;
    log.info "Received notification of SmartThings Home Monitor having changed status to ${shmState}"
    def mode = null
    switch (shmState) {
    	case 'off':
        	mode = 'Home'
            break
    	case 'stay':
        	mode = 'Stay'
            break
    	case 'away':
        	mode = 'Away'
            break
    }
    if (mode) {
		def children = getChildDevices()
    	children.each {
	    	//look for the digital-life-system device
	        if ((it.currentValue('module') == 'digitallife') && (it.currentValue('type') == 'digital-life-system')) {
				if (mode != it.currentValue('mode')) {
                	log.info 'Requesting mode ' + mode + ' from ' + it.name
                    proxyCommand it, 'mode', mode
                }
	        }
        }
 	}
    return true;
}

def modeChangeHandler(event) {
    if (event.name == 'mode') {
        log.info "Received notification of SmartThings Mode having changed to ${event.value}"
        def mode = null;
        switch (event.value) {
            case 'Home':
                mode = 'Home'
                break
            case 'Night':
                mode = 'Stay'
                break
            case 'Away':
                mode = 'Away'
                break
        }
        if (mode) {
            def children = getChildDevices()
            children.each {
                //look for the digital-life-system device
                if ((it.currentValue('module') == 'digitallife') && (it.currentValue('type') == 'digital-life-system')) {
                    if (mode != it.currentValue('mode')) {
                        log.info 'Requesting mode ' + mode + ' from ' + it.name
                        proxyCommand it, 'mode', mode
                    }
                }
            }
        }
        return true;
	}
}

private searchForLocalServer() {
    if(!state.ihch.subscribed) {
		subscribe(location, null, lanEventHandler, [filterEvents:false])
		state.ihch.subscribed = true
    }       
	log.trace "Looking for local HCH server..."
	sendHubCommand(new physicalgraph.device.HubAction("lan discovery " + getLocalServerURN(), physicalgraph.device.Protocol.LAN))
}

def lanEventHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

	def parsedEvent = parseLanMessage(description)

	//discovery
	if (parsedEvent.ssdpTerm && parsedEvent.ssdpTerm.contains(getLocalServerURN())) {
        atomicState.hchLocalServerIp = convertHexToIP(parsedEvent.networkAddress)
	}
    
    //ping response
    if (parsedEvent.data && parsedEvent.data.service && (parsedEvent.data.service == "hch")) {
	    def msg = parsedEvent.data
        if (msg.result == "pong") {
        	//log in successful to local server
            log.info "Successfully contacted local server"
			atomicState.hchPong = true
        }   	
    }
    if (parsedEvent.data && parsedEvent.data.event) {
        switch (parsedEvent.data.event) {
        	case "init":
                sendLocalServerCommand state.hch.localServerIp, "init", [
                            server: getHubLanEndpoint(),
                            modules: processSecurity([module: parsedEvent.data.data])
                        ]
				break
        	case "event":
            	processEvent(parsedEvent.data.data);
                break
        }
    }
    
}

private sendLocalServerCommand(ip, command, payload) {
    sendHubCommand(new physicalgraph.device.HubAction(
        method: "GET",
        path: "/${command}",
        headers: [
            HOST: "${ip}:42457"
        ],
        query: payload ? [payload: groovy.json.JsonOutput.toJson(payload).bytes.encodeBase64()] : []
    ))
}


private getHubLanEndpoint() {
	def server = [:]
	location.hubs.each {
	    //look for enabled modules
        def ip = it?.localIP
        def port = it?.localSrvPortTCP
        if (ip && port) {
        	log.trace "Found local endpoint at ${ip}:${port}"
        	server.ip = ip
            server.port = port
            return server
        }
    }
    return server
}










/***********************************************************************/
/*                      EXTERNAL EVENT MAPPINGS                        */
/***********************************************************************/
mappings {
    path("/event") {
        action: [
            GET: "processEvent",
            PUT: "processEvent"
        ]
    }
    path("/security") {
        action: [
            GET: "processSecurity"
        ]
    }
}












/***********************************************************************/
/*                      EXTERNAL EVENT HANDLERS                        */
/***********************************************************************/
private processEvent(data) {
	if (!data) {
    	data = params
    }

    def eventName = data?.event
    def eventValue = data?.value
    def deviceId = data?.id
    def deviceModule = data?.module
    def deviceName = data?.name.capitalize()
    def deviceType = data?.type
    def description = data?.description
    if (description) {
    	log.info 'Received event: ' + description
    } else {
    	log.info "Received ${eventName} event for module ${deviceModule}, device ${deviceName}, value ${eventValue}"
    }
	// see if the specified device exists and create it if it does not exist
    def deviceDNI = (deviceModule + '-' + deviceId).toLowerCase();
    def device = getChildDevice(deviceDNI)
    if(!device) {   
    	//build the device type
        def deviceHandler = null;
        //support for various modules
        switch (deviceModule) {
        	case 'digitallife':
            	deviceHandler = 'AT&T Digital Life ' + deviceType.replace('digital-life-', '').replace('-', ' ').split()*.capitalize().join(' ')
                break
        	case 'myq':
            	deviceHandler = 'MyQ ' + deviceType.replace('GarageDoorOpener', 'Garage Door').replace('-', ' ').split()*.capitalize().join(' ')
                break
        }
        if (deviceHandler) {
        	// we have a valid device type, create it
            try {
        		device = addChildDevice("ady624", deviceHandler, deviceDNI, null, [label: deviceName])
        		device.sendEvent(name: 'id', value: deviceId);
        		device.sendEvent(name: 'module', value: deviceModule);
        		device.sendEvent(name: 'type', value: deviceType);
            } catch(e) {
            	log.info 'Home Cloud Hub discovered a device that is not yet supported by your hub. Please find and install the [' + deviceHandler + '] device handler from https://github.com/ady624/SmartThingsPublic/tree/master/devicetypes/ady624'
            }
        }
    }
    if (device) {
    	// we have a valid device that existed or was just created, now set the state
        for(param in data) {
            def key = param.key
        	def value = param.value
        	if ((key.size() > 5) && (key.substring(0, 5) == 'data-')) {
            	key = key.substring(5);
                def oldValue = device.currentValue(key);
                if (oldValue != value) {
                    device.sendEvent(name: key, value: value);
                    //list of capabilities
                    //http://docs.smartthings.com/en/latest/capabilities-reference.html

                    //digital life alarm sync
                    if (true) {
                        if ((deviceType == 'digital-life-system') && (key == 'system-status')) {
                            log.info "Digital Life alarm status changed from ${oldValue} to ${value}"
                            def mode = null;
                            def shmState = null;
                            switch (value) {
                                case 'Home':
                                    mode = 'Home'
                                    shmState = 'off'
                                    break
                                case 'Away':
                                    mode = 'Away'
                                    shmState = 'away'
                                    break
                                case 'Stay':
                                    mode = 'Night'
                                    shmState = 'stay'
                                    break
                                case 'Instant':
                                    mode = 'Night'
                                    shmState = 'stay'
                                    break                        
                            }
                            if (mode) {
                                //sync location mode
                                if ((mode != location.mode) || (mode != device.currentValue('mode'))) {
                                    log.info 'Switching mode from ' + location.mode + ' to ' + mode
	                                device.sendEvent(name: 'mode', value: mode);
                                    //device has "Location Mode" as capability - we don't need to set the location mode
                                    //location.setMode(mode);
                                }
                            }
                            //sync SmartThings Home Monitor
                            //def currentShmState = location.currentState('alarmSystemStatus')?.value
                            //if (shmState && (shmState != currentShmState)) {
                                //log.info 'Switching SmartThings Home Monitor from ' + currentShmState + ' to ' + shmState
                                //sendLocationEvent(name: 'alarmSystemStatus', value: shmState)
                            //}
                        }                    
                    }
                }
            }
        }
    }
    if (state.hch.usesIFTTT && (eventName == 'update') && deviceModule && deviceId && eventName && eventValue && (eventValue != 'undefined')) {
    	//we need to proxy the event to IFTTT
        try {
        	def trigger = (deviceModule + '-' + deviceId + '-' + eventValue).replace(" ", "-").toLowerCase();
            log.info "Sending event #${trigger} to IFTTT"
			httpGet("https://maker.ifttt.com/trigger/${trigger}/with/key/" + settings.iftttKey);
        } catch(e) {
        }
    }
}

private processSecurity(data) {
	if (!data) {
    	data = params
    }
    
	def module = data?.module
    if (module) {
		log.info "Received request for security tokens for module ${module}"
    } else {
		log.info "Received request for security tokens for all modules"
    }
	//we are provided an endpoint to which to send command requests
	state.hch.security.each {
	    //look for enabled modules
        def name = it?.key
        def config = it?.value
        if (config.enabled && ((name == module) || !module)) {
        	switch (name) {
            	case "digitallife":
                	doATTLogin(false, false)
                    break
            	case "myq":
                	doMyQLogin(false, false)
                    break
            }
	        config.age = (config.connected ? (now() - config.connected) / 1000 : null)
        }
    }
    log.info "Providing security tokens to Home Cloud Hub"
    if (module) {
        //we only requested one module for refresh
        def sl = [:]
        if (state.hch.security[module]) {
        	sl[module] = state.hch.security[module];
        }
        return sl;
    } else {
    	return state.hch.security;
    }
}












/***********************************************************************/
/*                       EXTERNAL COMMAND PROXY                        */
/***********************************************************************/
def proxyCommand(device, command, value) {
	//child devices will use us to proxy things over to the homecloudhub.com service
	def module = device.currentValue('module')
    try {
        return "cmd_${module}"(device, command, value, false)
    } catch(e) {
    	return "Error proxying command: ${e}"
    }
    /*
	def id = device.currentValue('id')
    if (id && module && state.hch.security[module] && state.hch.security[module].controllable) {
       	def uri = 'https://www.homecloudhub.com/endpoint/' + state.hch.endpoint + '/' + module + '/' + id + '/' + command + '/' + value.bytes.encodeBase64()
        try {
            httpGet(uri) { resp ->
                return true;
            }
        } catch (e) {
            log.error 'Oh oh, something went wrong while requesting command ' + uri + ': ' + e
        }
    }
    return(null
    */
}












/***********************************************************************/
/*                  AT&T DIGITAL LIFE MODULE COMMANDS                  */
/***********************************************************************/
def cmd_digitallife(device, command, value, retry) {
	//are we allowed to use ATT?
   	def module_name = "digitallife"
	if (!state.hch.useATT || !(state.hch.security && state.hch.security[module_name] && state.hch.security[module_name].controllable)) {
    	//we are either not using this module or we can't controll it
    	return "No permission to control AT&T Digital Life"
    }
	if (!doATTLogin(false, retry)) {
    	log.error "Failed sending command to AT&T Digital Life because we could not connect"
    }
    def cookies = ""   
    //the hard part, get the cookies
    state.hch.security[module_name].cookies.each {
        cookies = cookies + it.value + "; "
    }
    //using the cookies, retrieve the auth tokens
    def path = null
    def data = null
    switch (device.currentValue("type")) {
    	case "digital-life-system":
        	switch (command) {
            	case "mode":
                	path = "alarm"
                    data = [
                    		bypass: '',
                            status: value
                    	]
                	break;
            }
        	break;
    }
    def result = false
    def message = ""
    if (path && data) {
    	try {
            result = httpPostJson([
                uri: "https://my-digitallife.att.com/penguin/api/${state.hch.security[module_name].key}/${path}",
                headers: [
					"Referer": "https://my-digitallife.att.com/dl",
                    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36",
                    "DNT": "1",
                    "appKey": "TI_3198CF46D58D3AFD_001",
                    "authToken": state.hch.security[module_name].authToken,
                    "requestToken": state.hch.security[module_name].requestToken,
                    "Cookie": cookies
                ],
                body: data
            ]) { response ->
                //check response, continue if 200 OK
                message = response.data
                if (response.status == 200) {
                	return true
                }
                return false
            }
            if (result) {
            	return "Successfuly sent command to AT&T Digital Life: device ${device} command ${command} value ${value} result ${message}"
            } else {
            	//if we failed and this was already a retry, give up
            	if (retry) {
		            return "Failed sending command to AT&T Digital Life: ${message}"
                }
            	//we failed the first time, let's retry
                return "cmd_${module_name}"(device, command, value, true)
            }
		} catch(e) {
    		message = "Failed sending command to AT&T Digital Life: ${e}"
        }
    }
}












/***********************************************************************/
/*                          MYQ MODULE COMMANDS                        */
/***********************************************************************/
def cmd_myq(device, command, value, retry) {
	//are we allowed to use MyQ?
   	def module_name = "myq"
	if (!state.hch.useMyQ || !(state.hch.security && state.hch.security[module_name] && state.hch.security[module_name].controllable)) {
    	//we are either not using this module or we can't controll it
    	return "No permission to control MyQ"
    }
	if (!doMyQLogin(false, retry)) {
    	log.error "Failed sending command to MyQ because we could not connect"
    }
	//using the cookies, retrieve the auth tokens
    def attrName = null
    def attrValue = null
    switch (device.currentValue("type")) {
    	case "GarageDoorOpener":
        	switch (command) {
            	case "open":
                	attrName = "desireddoorstate"
                	attrValue = 1
                	break;
            	case "close":
                	attrName = "desireddoorstate"
                	attrValue = 0
                	break;
            }
        	break;
    }
    def result = false
    def message = ""
    if (attrName && attrValue != null) {
    	try {
            result = httpPutJson([
                uri: "https://myqexternal.myqdevice.com/api/v4/deviceAttribute/putDeviceAttribute?appId=" + getMyQAppId() + "&securityToken=${state.hch.security[module_name].securityToken}",
                headers: [
                    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36"
                ],
                body: [
					ApplicationId: getMyQAppId(),
					SecurityToken: state.hch.security[module_name].securityToken,
                    MyQDeviceId: device.currentValue('id'),
					AttributeName: attrName,
                    AttributeValue: attrValue
                ]
            ]) { response ->
                //check response, continue if 200 OK
                message = response.data
                if ((response.status == 200) && response.data && (response.data.ReturnCode == 0)) {
                	return true
                }
                return false
            }
            if (result) {
            	return "Successfuly sent command to MyQ: device [${device}] command [${command}] value [${value}] result [${message}]"
            } else {
            	//if we failed and this was already a retry, give up
            	if (retry) {
		            return "Failed sending command to MyQ: ${message}"
                }
            	//we failed the first time, let's retry
                return "cmd_${module_name}"(device, command, value, true)
            }
		} catch(e) {
    		message = "Failed sending command to MyQ: ${e}"
        }
    }
}
