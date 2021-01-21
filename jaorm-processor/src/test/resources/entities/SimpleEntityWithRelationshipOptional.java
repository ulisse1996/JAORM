package io.test;

import java.util.Optional;
import io.jaorm.processor.annotation.Column;
import io.jaorm.processor.annotation.Relationship;
import io.jaorm.processor.annotation.Table;
import io.test.RelationshipTest;

@Table(name = "TABLE")
public class SimpleEntityWithRelationshipOptional {

    @Column(name = "COL1")
    private String col1;

    @Column(name = "COL2")
    private String col2;

    @Relationship(columns = @Relationship.RelationshipColumn(targetColumn = "TESTREL", defaultValue = "COL1"))
    private Optional<RelationshipTest> col3;

    public Optional<RelationshipTest> getCol3() {
        return col3;
    }

    public void setCol3(Optional<RelationshipTest> col3) {
        this.col3 = col3;
    }

    public String getCol2() {
        return col2;
    }

    public String getCol1() {
        return col1;
    }

    public void setCol1(String col1) {
        this.col1 = col1;
    }

    public void setCol2(String col2) {
        this.col2 = col2;
    }
}