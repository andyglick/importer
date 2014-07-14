/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.tagger.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.norconex.commons.lang.config.ConfigurationLoader;
import com.norconex.commons.lang.config.IXMLConfigurable;
import com.norconex.importer.ImporterMetadata;
import com.norconex.importer.handler.ImporterHandlerException;
import com.norconex.importer.tagger.IDocumentTagger;

/**
 * Copies metadata fields. If a target field already
 * exists, the values of the original field name will be <i>added</i>, unless
 * "overwrite" is set to <code>true</code>. 
 * <p/>
 * Can be used both as a pre-parse or post-parse handler.
 * <p/>
 * XML configuration usage:
 * </p>
 * <pre>
 *  &lt;tagger class="com.norconex.importer.tagger.impl.CopyTagger"&gt;
 *      &lt;copy from="(from field)" to="(to field)" overwrite="[false|true]" /&gt
 *      &lt;-- multiple copy tags allowed --&gt;
 *  &lt;/tagger&gt;
 * </pre>
 * 
 * @author Pascal Dimassimo
 * @author Pascal Essiembre
 * @since 1.3.0
 */
public class CopyTagger implements IDocumentTagger, IXMLConfigurable {

    private static final long serialVersionUID = -1880560826072410359L;

    private static class CopyDetails {
        private String from;
        private String to;
        private boolean overwrite;
        
        CopyDetails(String from, String to, boolean overwrite) {
            this.from = from;
            this.to = to;
            this.overwrite = overwrite;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + (overwrite ? 1231 : 1237);
            result = prime * result + ((to == null) ? 0 : to.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CopyDetails other = (CopyDetails) obj;
            if (from == null) {
                if (other.from != null)
                    return false;
            } else if (!from.equals(other.from))
                return false;
            if (overwrite != other.overwrite)
                return false;
            if (to == null) {
                if (other.to != null)
                    return false;
            } else if (!to.equals(other.to))
                return false;
            return true;
        }

        @Override
        public String toString() {
            ToStringBuilder builder = new ToStringBuilder(
                    this, ToStringStyle.SHORT_PREFIX_STYLE);
            builder.append("from", from);
            builder.append("to", to);
            builder.append("overwrite", overwrite);
            return builder.toString();
        }
        
    }
    
    
    private final List<CopyDetails> list = new ArrayList<CopyDetails>();

    @Override
    public void tagDocument(String reference, InputStream document,
            ImporterMetadata metadata, boolean parsed) 
                    throws ImporterHandlerException {
        for (CopyDetails details : list) {
            for (String value : metadata.getStrings(details.from)) {
                if (details.overwrite) {
                    metadata.setString(details.to, value);
                } else {
                    metadata.addString(details.to, value);
                }
            }
        }
    }
    
    /**
     * Adds copy instructions.
     * @param from source field name 
     * @param to target field name
     * @param overwrite whether to overwrite target field if it exists
     */
    public void addCopyDetails(
            String from, String to, boolean overwrite) {
        if (StringUtils.isNotBlank(from) 
                && StringUtils.isNotBlank(to)) {
            list.add(new CopyDetails(from, to, overwrite));
        }
    }
    
    @Override
    public void loadFromXML(Reader in) throws IOException {
        XMLConfiguration xml = ConfigurationLoader.loadXML(in);
        List<HierarchicalConfiguration> nodes =
                xml.configurationsAt("copy");
        for (HierarchicalConfiguration node : nodes) {
            addCopyDetails(node.getString("[@from]", null),
                      node.getString("[@to]", null),
                      node.getBoolean("[@overwrite]", false));
        }
    }
    
    @Override
    public void saveToXML(Writer out) throws IOException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            XMLStreamWriter writer = factory.createXMLStreamWriter(out);
            writer.writeStartElement("tagger");
            writer.writeAttribute("class", getClass().getCanonicalName());
            
            for (CopyDetails details : list) {
                writer.writeStartElement("copy");
                writer.writeAttribute("from", details.from);
                writer.writeAttribute("to", details.to);
                writer.writeAttribute("overwrite", 
                        Boolean.toString(details.overwrite));
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IOException("Cannot save as XML.", e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((list == null) ? 0 : list.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CopyTagger other = (CopyTagger) obj;
        if (list == null) {
            if (other.list != null)
                return false;
        } else if (!list.equals(other.list))
            return false;
        return true;
    }

    @Override
    public String toString() {
        ToStringBuilder builder = new ToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE);
        builder.append("list", list);
        return builder.toString();
    }
}
