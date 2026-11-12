<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">
    <!-- Begin with the document root and select all entry elements -->
    <xsl:template match="/">
        <xsl:apply-templates select="//*[local-name()='entry']"/>
    </xsl:template>

    <xsl:template match="*[local-name()='entry']">
        <entry>
            <xsl:apply-templates select="@* | node()"/>
        </entry>
    </xsl:template>

    <!-- Copy attributes without namespace -->
    <xsl:template match="@*">
        <xsl:attribute name="{local-name()}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>

    <!-- Identity transform for children (no namespace) -->
    <xsl:template match="*">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>

    <!-- Copy text nodes -->
    <xsl:template match="text()">
        <xsl:value-of select="."/>
    </xsl:template>
</xsl:stylesheet>
