package org.labkey.test.pages.issues;

import com.google.common.collect.ImmutableList;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.Select;
import org.labkey.test.components.labkey.ReadOnlyFormItem;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.Select.Select;
import static org.labkey.test.components.labkey.FormItemFinder.FormItem;
import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

public abstract class BaseIssuePage<EC extends BaseIssuePage.ElementCache> extends LabKeyPage<EC>
{
    protected BaseIssuePage(WebDriver driver)
    {
        super(driver);
    }

    public FormItem<String> status()
    {
        return elementCache().status;
    }

    public FormItem<String> assignedTo()
    {
        return elementCache().assignedTo;
    }

    public FormItem<String> priority()
    {
        return elementCache().priority;
    }

    public FormItem<String> related()
    {
        return elementCache().related;
    }

    public FormItem<String> resolution()
    {
        return elementCache().resolution;
    }

    public FormItem<String> duplicate()
    {
        return elementCache().duplicate;
    }

    public FormItem<String> notifyList()
    {
        return elementCache().notifyList;
    }

    public FormItem getCustomField(String label)
    {
        return elementCache().formItemWithLabel(label);
    }

    public String openedDate()
    {
        return (String) elementCache().openedDate.get();
    }

    public String closedDate()
    {
        return (String) elementCache().closedDate.get();
    }

    public String changedDate()
    {
        return (String) elementCache().changedDate.get();
    }

    public String resolvedDate()
    {
        return (String) elementCache().resolvedDate.get();
    }

    public List<IssueComment> getComments()
    {
        return elementCache().getComments();
    }

    public String getIssueId()
    {
        return WebTestHelper.parseUrlQuery(getURL()).get("issueId");
    }

    protected abstract EC newElementCache();

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final Map<String, FormItem> formItems = new CaseInsensitiveHashMap<>();

        protected FormItem status = formItemWithLabel("Status");
        protected FormItem assignedTo = formItemWithLabel("Assigned\u00a0To");
        protected FormItem priority = formItemWithLabel("Pri");
        protected FormItem resolution = formItemWithLabel("Resolution");
        protected FormItem duplicate = formItemWithLabel("Duplicate");
        protected FormItem related = formItemWithLabel("Related");
        protected FormItem notifyList = formItemWithLabel("Notify");
        protected FormItem openedDate = formItemWithLabel("Opened");
        protected FormItem changedDate = formItemWithLabel("Changed");
        protected FormItem resolvedDate = formItemWithLabel("Resolved");
        protected FormItem closedDate = formItemWithLabel("Closed");

        private FormItem replaceIfNewer(String nameOrLabel, FormItem candidate)
        {
            String key = nameOrLabel.replaceAll("\\s", "");
            FormItem formItem = formItems.get(key);
            if (formItem == null || !(candidate.getClass().isAssignableFrom(formItem.getClass())))
                formItems.put(key, candidate); // Replace with more specific or different FormItem
            return formItems.get(key);
        }

        protected FormItem formItemWithLabel(String label)
        {
            return replaceIfNewer(label, FormItem(getDriver()).withLabel(label.replaceAll(" ", "\u00a0")).findWhenNeeded(this));
        }

        protected FormItem formItemNamed(String name)
        {
            return replaceIfNewer(name, FormItem(getDriver()).withName(name).findWhenNeeded(this));
        }

        protected ReadOnlyFormItem readOnlyItem(String label)
        {
            return (ReadOnlyFormItem) replaceIfNewer(label, ReadOnlyFormItem().withLabel(label.replaceAll(" ", "\u00a0")).findWhenNeeded(this));
        }

        protected Select getSelect(String name)
        {
            FormItem formItem = replaceIfNewer(name, Select(fieldLocator(name)).findWhenNeeded(this));
            return (Select) formItem;
        }

        protected Input getInput(String name)
        {
            FormItem formItem = replaceIfNewer(name, Input(fieldLocator(name), getDriver()).findWhenNeeded(this));
            return (Input) formItem;
        }

        // Compensate for inconsistent name casing
        private Locator fieldLocator(String name)
        {
            return Locator.css(String.format("*[name=%s], *[name=%s]", name, name.toLowerCase()));
        }

        private List<IssueComment> issueComments;
        protected List<IssueComment> getComments()
        {
            if (issueComments == null)
            {
                List<WebElement> commentEls = Locator.css("div.currentIssue").findElements(this);
                issueComments = new ArrayList<>();
                for (WebElement commentEl : commentEls)
                {
                    issueComments.add(new IssueComment(commentEl));
                }
                issueComments = ImmutableList.copyOf(issueComments);
            }

            return issueComments;
        }
    }

    public class IssueComment
    {
        private final WebElement component;
        private String user;
        private String timestamp;
        private String comment;
        private Map<String, String> fieldChanges;

        IssueComment(WebElement component)
        {
            this.component = component;
        }

        public String getUser()
        {
            if (user == null)
                user = Locator.css(".comment-created-by").findElement(component).getText();
            return user;
        }

        public String getTimestamp()
        {
            if (timestamp == null)
                timestamp = Locator.css(".comment-created").findElement(component).getText();
            return timestamp;
        }

        public String getComment()
        {
            if (comment == null)
                comment = Locator.css(".labkey-wiki").findElement(component).getText();
            return comment;
        }

        public Map<String, String> getFieldChanges()
        {
            if (fieldChanges == null)
            {
                fieldChanges = new HashMap<>();
                List<String> changes = getTexts(Locator.css(".issues-Changes tr").findElements(component));

                for (String change : changes)
                {
                    Pattern pattern = Pattern.compile("(.+)\uc2bb(.*)");
                    Matcher matcher = pattern.matcher(change);

                    if (matcher.find())
                    {
                        String field = matcher.group(1).trim();
                        String value = matcher.group(2).trim();
                        fieldChanges.put(field, value);
                    }
                }
            }
            return fieldChanges;
        }
    }
}