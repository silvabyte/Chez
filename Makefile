


.PHONY: run-example-basic-usage
run-example-basic-usage:
	./mill Chez.runMain chez.examples.BasicUsage

.PHONY: run-example-complex-types
run-example-complex-types:
	./mill Chez.runMain chez.examples.ComplexTypes

.PHONY: run-example-validation
run-example-validation:
	./mill Chez.runMain chez.examples.Validation

.PHONY: run-example-lihaoyi-ecosystem
run-example-lihaoyi-ecosystem:
	./mill Chez.runMain chez.examples.LihaoyiEcosystem

.PHONY: run-example-all
run-example-all:
	make run-example-basic-usage
	make run-example-complex-types
	make run-example-validation
	make run-example-lihaoyi-ecosystem