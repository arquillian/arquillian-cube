package org.arquillian.cube.spi.metadata;

public class IsBuildable {

    private String templatePath;

    public IsBuildable(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getTemplatePath() {
        return templatePath;
    }
}
