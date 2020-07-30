package oauth.server

import io.ghap.oauth.Client

import javax.servlet.http.HttpServletResponse

class BasicAuthFilterFilters {

    def springSecurityService

    def filters = {
        all(controller:'*', action:'*') {
            before = {

            }
            after = { Map model ->

            }
            afterView = { Exception e ->

            }
        }
        basicAuth(controller:'token', action:'tokenInfo|updateToken') {
            before = {
                def authString = request.getHeader('Authorization')

                if (!authString) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                    return false;
                }
                def encodedPair = authString - 'Basic '
                def decodedPair = new String(new sun.misc.BASE64Decoder().decodeBuffer(encodedPair));

                def credentials = decodedPair.split(':')
                try {
                    def client = Client.findByClientId(credentials[0]);
                    if (!client) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        return false;
                    }
                    if (!springSecurityService.passwordEncoder.isPasswordValid(client.clientSecret, credentials[1], null)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                        return false;
                    }
                    params.client = client
                    return true;
                } catch (Throwable e) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                    return false;
                }
            }
            after = { Map model ->

            }
            afterView = { Exception e ->

            }
        }
    }
}
