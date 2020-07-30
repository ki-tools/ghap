<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    </head>
    <body style="white-space:initial;margin: 0; padding: 0;font-family: Helvetica, sans-serif;">
        <p>Dear $user.fullName$,</p>
        <p>
            This email confirms that changes were made to the following attributes of your account in the GHAP system:
            <ul>
                $changes: {change|
                    <li>
                        $if(change.hide)$
                            $change.field$ was changed
                        $else$
                            $change.field$: $change.newValue$
                        $endif$
                    </li>
                }$
            </ul>
        </p>
        <p>
            If you did not make this change,
            please contact the GHAP administrator by emailing
            <a href="mailto:$adminEmail$">$adminEmail$</a>.
        </p>
        <p>Thanks,<br>GHAP System Administration</p>
    </body>
</html>