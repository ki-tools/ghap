package io.ghap.ldap;


import io.ghap.auth.LdapConfiguration;
import io.ghap.ldap.template.LdapConnectionTemplate;
import net.tirasa.adsddl.ntsd.ACE;
import net.tirasa.adsddl.ntsd.SDDL;
import net.tirasa.adsddl.ntsd.SID;
import net.tirasa.adsddl.ntsd.controls.SDFlagsControl;
import net.tirasa.adsddl.ntsd.data.AceObjectFlags;
import net.tirasa.adsddl.ntsd.data.AceRights;
import net.tirasa.adsddl.ntsd.data.AceType;
import net.tirasa.adsddl.ntsd.utils.GUID;
import net.tirasa.adsddl.ntsd.utils.NumberFacility;
import net.tirasa.adsddl.ntsd.utils.SDDLHelper;
import org.apache.directory.api.asn1.DecoderException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Control;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.directory.SearchControls;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Singleton
public class Permission {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject LdapConnectionFactory ldapConnectionFactory;
    @Inject LdapConfiguration ldapConfiguration;

    public void userCanChangePassword(String userDn) throws LdapException {

        try (LdapConnection ldap = ldapConnectionFactory.get(ldapConfiguration.getAdmin())) {
            final SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(new String[]{"nTSecurityDescriptor"});

            LdapConnectionTemplate connectionTemplate = new LdapConnectionTemplate(ldap);
            Control sdFlagControl = ldap.getCodecService().fromJndiControl(new SDFlagsControl(0x00000004));

            final Dn dn = connectionTemplate.newDn(userDn);

            Entry ent = connectionTemplate.lookup(dn, new Control[]{sdFlagControl}, "nTSecurityDescriptor");

            Value nTSecurityDescriptor = ent.get("nTSecurityDescriptor").get();
            final byte[] orig = nTSecurityDescriptor.getBytes();

            SDDL sddl = new SDDL(orig);

            ModifyRequest modifyRequest = new ModifyRequestImpl()
                    .setName(dn)
                    .replace("ntSecurityDescriptor", userCannotChangePassword(sddl, false).toByteArray());

            connectionTemplate.modify(modifyRequest);

            // test modification results
            sdFlagControl = ldap.getCodecService().fromJndiControl(new SDFlagsControl(0x00000001 + 0x00000002 + 0x00000004 + 0x00000008));
            ent = connectionTemplate.lookup(
                    dn,
                    new Control[]{sdFlagControl},
                    "nTSecurityDescriptor");

            final byte[] changed = ent.get("nTSecurityDescriptor").get().getBytes();
            sddl = new SDDL(changed);

            boolean isUserCannotChangePassword = SDDLHelper.isUserCannotChangePassword(sddl);

            if(isUserCannotChangePassword){
                log.error("User \""+dn+"\" still cannot change password");
            }

            //System.out.println(isUserCannotChangePassword);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private SDDL userCannotChangePassword(final SDDL sddl, final boolean cannot) {
        final AceType type = cannot ? AceType.ACCESS_DENIED_OBJECT_ACE_TYPE : AceType.ACCESS_ALLOWED_OBJECT_ACE_TYPE;

        ACE self = null;
        ACE all = null;

        final List<ACE> aces = sddl.getDacl().getAces();
        for (int i = 0; (all == null || self == null) && i < aces.size(); i++) {
            final ACE ace = aces.get(i);

            if ((ace.getType() == AceType.ACCESS_ALLOWED_OBJECT_ACE_TYPE
                    || ace.getType() == AceType.ACCESS_DENIED_OBJECT_ACE_TYPE)
                    && ace.getObjectFlags().getFlags().contains(AceObjectFlags.Flag.ACE_OBJECT_TYPE_PRESENT)) {
                if (GUID.getGuidAsString(ace.getObjectType()).equals(SDDLHelper.UCP_OBJECT_GUID)) {

                    final SID sid = ace.getSid();
                    if (sid.getSubAuthorities().size() == 1) {
                        if (self == null && Arrays.equals(
                                sid.getIdentifierAuthority(), new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x01 })
                                && Arrays.equals(
                                sid.getSubAuthorities().get(0), new byte[] { 0x00, 0x00, 0x00, 0x00 })) {
                            self = ace;
                            self.setType(type);
                        } else if (all == null && Arrays.equals(
                                sid.getIdentifierAuthority(), new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x05 })
                                && Arrays.equals(
                                sid.getSubAuthorities().get(0), new byte[] { 0x00, 0x00, 0x00, 0x0a })) {
                            all = ace;
                            all.setType(type);
                        }
                    }
                }
            }
        }

        if (self == null) {
            // prepare aces
            self = ACE.newInstance(type);
            self.setObjectFlags(new AceObjectFlags(AceObjectFlags.Flag.ACE_OBJECT_TYPE_PRESENT));
            self.setObjectType(GUID.getGuidAsByteArray(SDDLHelper.UCP_OBJECT_GUID));
            self.setRights(new AceRights().addOjectRight(AceRights.ObjectRight.CR));
            SID sid = SID.newInstance(NumberFacility.getBytes(0x000000000001));
            sid.addSubAuthority(NumberFacility.getBytes(0));
            self.setSid(sid);
            sddl.getDacl().getAces().add(self);
        }

        if (all == null) {
            all = ACE.newInstance(type);
            all.setObjectFlags(new AceObjectFlags(AceObjectFlags.Flag.ACE_OBJECT_TYPE_PRESENT));
            all.setObjectType(GUID.getGuidAsByteArray(SDDLHelper.UCP_OBJECT_GUID));
            all.setRights(new AceRights().addOjectRight(AceRights.ObjectRight.CR));
            final SID sid = SID.newInstance(NumberFacility.getBytes(0x000000000005));
            sid.addSubAuthority(NumberFacility.getBytes(0x0A));
            all.setSid(sid);
            sddl.getDacl().getAces().add(all);
        }

        return sddl;
    }
}
