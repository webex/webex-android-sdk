package com.ciscospark.androidsdk.membership;

import com.google.common.collect.Collections2;
import com.google.gson.Gson;

import org.apache.tools.ant.util.Base64Converter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by zhiyuliu on 31/08/2017.
 */

public class MembershipTest {

    @Test
    public void testConvert() {
        String jsonString = "{\n" +
                "  \"id\" : \"Y2lzY29zcGFyazovL3VzL01FTUJFUlNISVAvMGQwYzkxYjYtY2U2MC00NzI1LWI2ZDAtMzQ1NWQ1ZDExZWYzOmNkZTFkZDQwLTJmMGQtMTFlNS1iYTljLTdiNjU1NmQyMjA3Yg\",\n" +
                "  \"roomId\" : \"Y2lzY29zcGFyazovL3VzL1JPT00vYmJjZWIxYWQtNDNmMS0zYjU4LTkxNDctZjE0YmIwYzRkMTU0\",\n" +
                "  \"personId\" : \"Y2lzY29zcGFyazovL3VzL1BFT1BMRS9mNWIzNjE4Ny1jOGRkLTQ3MjctOGIyZi1mOWM0NDdmMjkwNDY\",\n" +
                "  \"personEmail\" : \"john.andersen@example.com\",\n" +
                "  \"personDisplayName\" : \"John Andersen\",\n" +
                "  \"personOrgId\" : \"Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi85NmFiYzJhYS0zZGNjLTExZTUtYTE1Mi1mZTM0ODE5Y2RjOWE\",\n" +
                "  \"isModerator\" : true,\n" +
                "  \"isMonitor\" : true,\n" +
                "  \"created\" : \"2015-10-18T14:26:16.203Z\"\n" +
                "}";
        Gson gson = new Gson();
        Membership membership = gson.fromJson(jsonString, Membership.class);
        assertNotNull(membership);
        assertEquals(membership.getId(), "Y2lzY29zcGFyazovL3VzL01FTUJFUlNISVAvMGQwYzkxYjYtY2U2MC00NzI1LWI2ZDAtMzQ1NWQ1ZDExZWYzOmNkZTFkZDQwLTJmMGQtMTFlNS1iYTljLTdiNjU1NmQyMjA3Yg");
        assertEquals(membership.getPersonEmail(), "john.andersen@example.com");
        assertEquals(membership.getPersonDisplayName(), "John Andersen");
        assertEquals(membership.getPersonOrgId(), "Y2lzY29zcGFyazovL3VzL09SR0FOSVpBVElPTi85NmFiYzJhYS0zZGNjLTExZTUtYTE1Mi1mZTM0ODE5Y2RjOWE");
        assertEquals(membership.getRoomId(), "Y2lzY29zcGFyazovL3VzL1JPT00vYmJjZWIxYWQtNDNmMS0zYjU4LTkxNDctZjE0YmIwYzRkMTU0");
        assertEquals(membership.isModerator(), true);
        assertEquals(membership.isMonitor(), true);
        assertNotNull(membership.getCreated());
    }


}
