package com.andredurante.fruitmachine.config;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.andredurante.fruitmachine.service.*;
import com.andredurante.fruitmachine.storage.InMemoryMachineRepository;
@Configuration
public class MachineBeans {
    @Bean MachineRepository machineRepository() { return new InMemoryMachineRepository(); }
    @Bean MachineService machineService(MachineRepository repository) {
        return new MachineService(repository, Random::new);
    }
}
