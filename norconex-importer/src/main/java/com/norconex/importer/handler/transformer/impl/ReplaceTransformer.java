/* Copyright 2010-2017 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.handler.transformer.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.commons.lang.xml.EnhancedXMLStreamWriter;
import com.norconex.importer.doc.ImporterMetadata;
import com.norconex.importer.handler.transformer.AbstractStringTransformer;

/**
 * <p>Replaces every occurrences of the given replacements
 * (document content only).</p>
 * 
 * <p>This class can be used as a pre-parsing (text content-types only) 
 * or post-parsing handlers.</p>
 * 
 * <h3>XML configuration usage:</h3>
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ReplaceTransformer"
 *          caseSensitive="[false|true]"
 *          sourceCharset="(character encoding)"
 *          maxReadSize="(max characters to read at once)" &gt;
 *      
 *      &lt;restrictTo caseSensitive="[false|true]"
 *              field="(name of header/metadata field name to match)"&gt;
 *          (regular expression of value to match)
 *      &lt;/restrictTo&gt;
 *      &lt;!-- multiple "restrictTo" tags allowed (only one needs to match) --&gt;
 *          
 *      &lt;replace&gt;
 *          &lt;fromValue&gt;(regex of value to replace)&lt;/fromValue&gt;
 *          &lt;toValue&gt;(replacement value)&lt;/toValue&gt;
 *      &lt;/replace&gt;
 *      &lt;!-- multiple replace tags allowed --&gt;
 *      
 *  &lt;/transformer&gt;
 * </pre>
 * <p>
 * <b>Note:</b> To preserve white space add <code>xml:space="preserve"</code>
 * to the "toValue" tag, like this:
 * </p>
 * <pre>
 *   &lt;toValue xml:space="preserve"&gt; &lt;/toValue&gt;
 * </pre>
 * 
 * <h4>Usage example:</h4>
 * <p>
 * The following reduces all occurrences of "junk food" with "healthy food".
 * </p> 
 * <pre>
 *  &lt;transformer class="com.norconex.importer.handler.transformer.impl.ReplaceTransformer"&gt;
 *      &lt;replace&gt;
 *          &lt;fromValue&gt;junk food&lt;/fromValue&gt;
 *          &lt;toValue&gt;healthy food&lt;/toValue&gt;
 *      &lt;/replace&gt;
 *  &lt;/transformer&gt;
 * </pre>
 * 
 * @author Pascal Essiembre
 * @since 1.2.0
 */
public class ReplaceTransformer extends AbstractStringTransformer
        implements IXMLConfigurable {

    private boolean caseSensitive;
    private final Map<String, String> replacements = new ListOrderedMap<>();

    @Override
    protected void transformStringContent(String reference,
            StringBuilder content, ImporterMetadata metadata, boolean parsed,
            int sectionIndex) {

        String text = content.toString();
        content.setLength(0);
        Pattern pattern;
        
        for (String from : replacements.keySet()) {
            String to = replacements.get(from);
            int flags = Pattern.DOTALL;
            if (!caseSensitive) {
                flags = flags | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            pattern = Pattern.compile(from, flags);
            text = pattern.matcher(text).replaceAll(to);
        }
        content.append(text);
    }
     
    public Map<String, String> getReplacements() {
        return ListOrderedMap.listOrderedMap(replacements);
    }
    public void addReplacement(String from, String to) {
        this.replacements.put(from, to);
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    /**
     * Sets whether to ignore case when matching characters or string
     * to reduce.
     * @param caseSensitive <code>true</code> to consider character case
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    @Override
    protected void loadStringTransformerFromXML(XMLConfiguration xml)
            throws IOException {
        setCaseSensitive(xml.getBoolean("[@caseSensitive]", false));

        List<HierarchicalConfiguration> nodes = 
                xml.configurationsAt("replace");
        for (HierarchicalConfiguration node : nodes) {
            replacements.put(
                    node.getString("fromValue"), node.getString("toValue"));
        }
    }
    
    @Override
    protected void saveStringTransformerToXML(EnhancedXMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeAttribute(
                "caseSensitive", Boolean.toString(isCaseSensitive()));
        for (String from : replacements.keySet()) {
            String to = replacements.get(from);
            writer.writeStartElement("replace");
            writer.writeStartElement("fromValue");
            writer.writeCharacters(from);
            writer.writeEndElement();
            writer.writeStartElement("toValue");
            writer.writeCharacters(to);
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(caseSensitive)
            .append(replacements)
            .toHashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ReplaceTransformer)) {
            return false;
        }
        ReplaceTransformer castOther = 
                (ReplaceTransformer) other;
        return new EqualsBuilder()
                .appendSuper(super.equals(castOther))
                .append(caseSensitive, castOther.caseSensitive)
                .append(replacements, castOther.replacements)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("caseSensitive", caseSensitive)
                .append("replacements", replacements).toString();
    }
}
