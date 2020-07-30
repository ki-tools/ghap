<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
	<title></title>
	<link rel="icon" href=""/>
	<asset:stylesheet src="application.css"/>
</head>

<style>
div.bannertext {
  text-align:center;
	color: white;
}

span.bannertextstyle {
    display: inline-block;
    vertical-align: middle;
    line-height: 35px;
  }

.shifted-for-banner {padding: 30px 0px; }

</style>

<body>
	<g:each in="${banners}">
		<div class="bannertext" style="background-color: ${it.color}">
			<span class="bannertextstyle"><b><u>${it.title}:</b></u> ${it.message}</span>
		</div>

	</g:each>


<div class="page-wrapper">
	<div class="header-wrapper">
		<div class="content-row top-nav-cont">
			<div class="content">
			</div>
		</div>

		<div class="content-row main-nav-cont">
			<div class="content">
				<div class="grid gutter">
					<div class="grid-col col-3-12">
						<a class="header-logo" href="/"><g:img dir="images" file="global-health-logo.0db8fc0f.png"/></a>
					</div>
				</div>
			</div>
		</div>

		<div class="content-row sub-nav-cont">
			<div class="content">
				<div class="sub-nav">
				</div>
			</div>
		</div>
	</div>

	<div class="content-wrapper clear page-login">
		<form action='${postUrl}' method='POST' id='loginForm' autocomplete='off'>
			<div class="content-row padded">
				<div class="content"></div>
			</div>

			<div class="content-row padded">
				<div class="content"></div>
			</div>

			<div class="content-row padded">
				<div class="content"></div>
			</div>

			<div class="content-row padded">
				<div class="content"></div>
			</div>

			<div class="content-row padded">
				<div class="content">
					<div class="grid">
						<div class="grid-col col-8-12"></div>

						<div class="grid-col col-4-12">
							<h2 class="color-quinary"><strong>Log In</strong></h2>
							<g:if test="${flash.loginError}">
								<div class="error-message-cont color-quinary">
									${flash.loginError}
								</div>
							</g:if>
							<g:if test="${userNotFound}">
								<div class="error-message-cont color-quinary">
									Log in failed. Please try again.
								</div>
							</g:if>
							<g:if test="${usernameRequired}">
								<div class="error-message-cont color-quinary">
									Username required.
								</div>
							</g:if>
							<g:if test="${passwordRequired}">
								<div class="error-message-cont color-quinary">
									Password required.
								</div>
							</g:if>
							<g:if test="${policyRequired}">
								<div class="error-message-cont color-quinary">
									You must accept privacy policy prior login.
								</div>
							</g:if>
							<div class="input-cont">
								<input class="text-input" type="text" name="j_username" placeholder="firstname.lastname">
							</div>

							<div class="input-cont">
								<input class="text-input" type="password" name="j_password" placeholder="Password">
							</div>
							<a href="${grailsApplication.config.ghap.forgot.pwd.page}"
							   class="weight-600 color-quinary"><u>Forgot your password?</u></a>
							<% /*
							<div class="input-cont user-policy-cont box-cont tan text-no-wrap">
								<label>
									<input class="v-align-top" type="checkbox" name="user-policy" id="user-policy">
									<span class="text-wrap v-align-top">
										<a href="#${grailsApplication.config.ghap.tos.page}">Privacy Policy</a>
										Selecting this check box certifies that you have read and accepted the terms of this policy.
									</span>
								</label>
							</div>
							*/ %>
						</div>
					</div>

					<div class="grid gutter">
						<div class="grid-col col-10-12"></div>

						<div class="grid-col col-2-12">
							<div class="input-cont align-right no-margin">
								<input class="submit-button" type="submit" value="LOG IN"/>
							</div>
						</div>
					</div>
				</div>
			</div>
		</form>
	</div>
	<script type="application/javascript">
		function getCookie(cname) {
			var name = cname + "=";
			var ca = document.cookie.split(';');
			for (var i = 0; i < ca.length; i++) {
				var c = ca[i];
				while (c.charAt(0) == ' ') c = c.substring(1);
				if (c.indexOf(name) == 0) return c.substring(name.length, c.length);
			}
			return "";
		}

		function checkCookie() {
			var ppread = getCookie("ppolicyread");
			if (ppread != "") {
				//document.getElementById("user-policy").checked = true;
			}
		}
		checkCookie();
	</script>

	<div class="footer-wrapper clear content-row">
		<div class="footer content">
			<div class="grid">
				<div class="grid-col col-1-2">
                	<p>Contact Support: <a href="mailto:support@ghap.io">support@ghap.io</a> or 314-951-3090</p>
                	<br>
                	<p>Support Hours: 12:00 AM to 6:00 PM US ET</p>
				</div>

				<div class="grid-col col-1-2 align-right">
					<% /* <a href="#${grailsApplication.config.ghap.tos.page}">Terms of Use and Privacy Policy</a> */ %>
				</div>
			</div>
		</div>
	</div>
</div>
</body>
</html>