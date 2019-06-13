package org.labkey.remoteapi.domain;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class CreateDomainCommand extends DomainCommand
{
    public CreateDomainCommand(String domainKind, String domainName)
    {
        super("property", "createDomain");
        setDomainKind(domainKind);
        setDomainName(domainName);
    }

    @Override
    public JSONObject getJsonObject()
    {
        JSONObject obj = new JSONObject();
        obj.put("schemaName", getSchemaName());
        obj.put("domainKind", getDomainKind());

        JSONArray fields = new JSONArray();
        fields.addAll(getColumns());

        JSONObject domainDesign = new JSONObject();
        domainDesign.put("name", getDomainName());
        domainDesign.put("fields", fields);

        obj.put("domainDesign", domainDesign);
        return obj;
    }
}
