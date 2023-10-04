package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.HelloWorld
import org.junit.Test
import kotlin.test.assertTrue

class HelloWorldTest {

    @Test
    fun test1(){
        val hello = HelloWorld();

        assertTrue(hello.property == 1)
    }

    @Test
    fun test2(){
        val hello = HelloWorld();
        hello.property = 2
        assertTrue(hello.property == 2)
    }

}