# Capybara - An analyzer for Java bytecode

An analyzer for Java bytecode, currently relying solely on "brute-force" symbolic execution.

This software has mostly been developed for learning/experimentation purposes. It has demonstrated its ability to find some bugs in example programs 
(see a toy example [here](src/test/resources/testprojects/TargetedTestCases/src/main/java/Foo.java)), but it is still very incomplete.


## Ideas

- [x] Intra-method analysis
- [x] Method summaries generation
- [ ] Exploit summaries of already analyzed methods
- [ ] Track the values in arrays, when not aliased
- [ ] Track the values in fields, when field is immutable or object is not aliased (requires to detect at least getters and setters)
- [ ] Detect which of its arguments a method can modify
- [ ] Support for typestates/protocols


## References

List of resources that are useful to this project:

- [A Survey of Symbolic Execution Techniques](https://arxiv.org/pdf/1610.00502)
- [Java ASM bytecode manipulation library](https://asm.ow2.io/), especially the analysis tree API
- [KSMT library](https://ksmt.io/)
