<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    </head>
    <body style="white-space:initial;margin: 0; padding: 0;">
    <span style="font-family:"Calibri">
        <p>Dear GHAP User,</p>
        <p>Our team monitors usage of virtual environments created on the <b>Global Health Analytics Platform</b> (GHAP) on a daily basis.  In doing so, we are able to identify Windows and Linux environments exhibiting a very low CPU utilization for 24 consecutive hours. We estimate that these environments have been left running, but are not being used to carry out modelling and analysis activities.</p>
        <p>In order to save costs, we will force a <b>STOP</b> of the environment(s) below on $scheduledStopTime$ – 12 hours after delivery of email notification, or after 36 hours of idle time. If you have completed and saved relevant work within your environment, no further action is needed. You can quickly <b>START</b> this environment(s) via the <i>Computing Environment page</i> within <a href="https://www.ghap.io">www.ghap.io</a> when ready. If you are actively working in this environment(s) and need to keep the environment(s) running, you can delay this forced <b>STOP</b> for another 24 hours by clicking the link(s) in the table below:</p>

              <table class="table table-bordered" border="0" cellspacing="0" cellpadding="0" style="width:50%;border-collapse:collapse">
                <thead>
                <tr>
                  <th valign="top" style="width:40%;border:solid white 1.0pt;border-right:none;background:#5B9BD5;">
                    <span style="color:white">Environment Type and Node</span>
                  </th>
                  <th valign="top" style="width:20%;border:solid white 1.0pt;border-right:none;background:#5B9BD5;">
                    <span style="color:white">IP Address</span>
                  </th>
                  <th valign="top" style="width:40%;border:solid white 1.0pt;border-right:none;background:#5B9BD5;">
                    <span style="color:white">Delay a Forced STOP?</span>
                  </th>
                </tr>
                </thead>
                <tbody>
                  <tr>
                    <td valign="top" style="border:solid white 1.0pt;background:#5B9BD5;">
                        <b><i><span style="color:white">$activityOsType$ - $activityName$</span></i></b>
                    </td>
                    <td valign="top" style="border:solid white 1.0pt;background:#5B9BD5;">$activityIPAddress$</td>
                    <td valign="top" style="border:solid white 1.0pt;background:#5B9BD5;">
                        <a href="$urlToPostponeScheduledStop$">I’m still working – don’t STOP my environment on $scheduledStopTime$</a>
                    </td>
                  </tr>
                </tbody>
              </table>


        <p>If you have any questions, please email GHAP Customer Support at <a href="mailto:$adminEmail$">$adminEmail$</a>.<br/>Thanks,<br/>GHAP System Administration</p>
        </span>
    </body>
</html>





