package io.ghap.oauth

import grails.transaction.Transactional
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.apache.commons.lang.time.StopWatch
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

class CheckHealthService {

	private static final String STATUS_FAILED = "Failed: ";

	def sessionFactory
	def grailsApplication
	def pwdAuthenticator

	Map<String, String> checkHealth() {
		Map<String, String> result = new HashMap<>();
		result.put("db", pingDb());
		result.put("ldap", pingLdap());
		result.put("/oauth/authorize", checkMethod("/oauth/authorize", GET));
		result.put("/oauth/tokeninfo", checkMethod("/oauth/tokeninfo", GET));
		result.put("/oauth/tokeninfo/update", checkMethod("/oauth/tokeninfo/update", POST));
		result.put("/oauth/revoke", checkMethod("/oauth/revoke", GET));
		return result;
	}

	boolean isCheckSuccess(Map<String, String> result) {
		Collection<String> values = new ArrayList<>(result.values());
		for (String val : values) {
			if (val.startsWith(STATUS_FAILED)) {
				return false;
			}
		}
		return true;
	}

	@Transactional(readOnly = true)
	private String pingDb() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		try {
			sessionFactory.currentSession.createSQLQuery("select 1").list();
		} catch (Throwable e) {
			return STATUS_FAILED + e.getMessage();
		} finally {
			stopWatch.stop();
		}
		return stopWatch.toString();
	}

	private String pingLdap() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		try {
			pwdAuthenticator.authenticate(new UsernamePasswordAuthenticationToken("test", "test"))
		} catch (UsernameNotFoundException e) {
			//do nothing
		} catch (BadCredentialsException e) {
			// do nothing
		} catch (Throwable e) {
			return STATUS_FAILED + e.message
		} finally {
			stopWatch.stop();
		}
		return stopWatch.toString();
	}

	private String checkMethod(String methodPath, Method method) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		try {
			HTTPBuilder http = new HTTPBuilder(grailsApplication.config.grails.serverURL + methodPath)
			def remoteResult = http.request(method) { req ->
                //timeouts
                response.success = { resp, json ->
                    return true
                }
                // status >= 400
                response.failure = { resp, json ->
                    checkResponse(resp);
                    return false
                }
            }
		} catch (Throwable e) {
			return STATUS_FAILED + e.message
		} finally {
			stopWatch.stop();
		}
		return stopWatch.toString();
	}

	private void checkResponse(def resp) throws Exception {
		int status = resp.status;
		if (status >= 400 && status != 401 && status != 405) {
			throw new Exception("method returns " + status);
		}
	}
}
