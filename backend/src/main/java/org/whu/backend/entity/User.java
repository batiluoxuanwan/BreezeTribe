package org.whu.backend.entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // 告诉JPA这是一个实体类
@Table(name = "test_users") // 可选，如果表名和类名不一致（通常建议用复数形式的表名）
@Data // Lombok注解，自动生成getter/setter等
@NoArgsConstructor
@AllArgsConstructor
public class User {

//    @Column注解一共有10个属性，这10个属性均为可选属性，各属性含义分别如下：
//
//    name：定义了被标注字段在数据库表中所对应字段的名称。
//    unique：该字段是否为唯一标识，默认为false。也可以使用@Table标记中的@UniqueConstraint。
//    nullable：该字段是否可以为null值，默认为true。
//    length：字段的长度，当字段的类型为varchar时，该属性才有效，默认为255个字符。
//    insertable：在使用“INSERT”脚本插入数据时，是否插入该字段的值。默认为true。
//    updatable：在使用“UPDATE”脚本插入数据时，是否更新该字段的值。默认为true。
//    insertable = false 和updatable = false 一般多用于只读的属性，例如主键和外键等。这些字段的值通常是自动生成的。
//
//    columnDefinition：建表时创建该字段的DDL语句，一般用于通过Entity生成表定义时使用。
//            （如果DB中表已经建好，该属性没有必要使用）
//
//    precision和scale：表示精度，当字段类型为double时，precision表示数值的总长度，scale表示小数点所占的位数。
//    table：定义了包含当前字段的表名。


    @Id // 标记主键
    @Column(length =  36)
    private String id; // 我们之前讨论过用UUID作为ID，这里用String

    private String name;
    private String email;
    private String hashedPassword;
    private String test;

    // 如果用JPA的 @GeneratedValue 来生成自增ID (但我们现在讨论的是UUID)
    // @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    // private Long id;
}