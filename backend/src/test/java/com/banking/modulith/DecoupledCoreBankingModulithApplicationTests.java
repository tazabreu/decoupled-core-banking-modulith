package com.banking.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class DecoupledCoreBankingModulithApplicationTests {

	@Test
	void contextLoads() {
	}

	ApplicationModules modules = ApplicationModules.of(DecoupledCoreBankingModulithApplication.class);

	@Test
	void verifyModularStructure() {
		modules.verify();
	}

	@Test
	void generateModuleDocumentation() {
		new Documenter(modules)
				.writeDocumentation()
				.writeIndividualModulesAsPlantUml()
				.writeModulesAsPlantUml();
	}

}
