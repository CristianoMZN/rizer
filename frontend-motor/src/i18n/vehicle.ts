// Mapeamento de chaves de `products.attributes` (JSONB) para rótulos
// em pt-BR exibidos na ficha técnica do veículo. Mantido centralizado
// para que a UI e a documentação compartilhem a mesma nomenclatura.

export const ATTRIBUTE_LABELS: Record<string, string> = {
  // Gerais
  color: 'Cor',
  doors: 'Portas',
  engine: 'Motor',
  vin: 'Chassi',
  license_plate: 'Placa',
  previous_owners: 'Único dono',
  warranty_months: 'Garantia (meses)',
  factory_warranty: 'Garantia de fábrica',
  mechanical_warranty: 'Garantia mecânica',
  inspection_done: 'Vistoria',

  // Motor / mecânica
  cylinders: 'Cilindros',
  cylinder_layout: 'Posição do cilindro',
  engine_displacement_l: 'Motorização',
  engine_cc: 'Cilindrada',
  power_hp: 'Potência',
  torque_nm: 'Torque',
  valves_per_cylinder: 'Válvulas por cilindro',
  fuel_supply: 'Alimentação',
  fuel_tank_l: 'Tanque de combustível',
  consumption_city: 'Consumo cidade (km/l)',
  consumption_highway: 'Consumo estrada (km/l)',
  top_speed_kmh: 'Velocidade máxima (km/h)',
  zero_to_hundred_s: '0 a 100 (s)',

  // Dimensões
  length_mm: 'Comprimento (mm)',
  width_mm: 'Largura (mm)',
  height_mm: 'Altura (mm)',
  wheelbase_mm: 'Distância entre eixos (mm)',
  weight_kg: 'Peso (kg)',
  trunk_l: 'Porta-malas (L)',
  seats: 'Quantidade de pessoas',
  body_type: 'Tipo de carroceria',

  // Trem de força
  drivetrain: 'Tração',
  steering: 'Direção',
  transmission_detail: 'Transmissão',
  gears: 'Marchas',
  armored: 'Blindado',
  armored_level: 'Nível de blindagem',

  // Estado / negociação
  condition: 'Condição',
  accept_trade: 'Aceita troca',
  financed: 'Financiado',
  ipva_paid: 'IPVA pago',
  licensing_year: 'Licenciamento',
  last_plate_digit: 'Último dígito da placa',

  // Segurança
  abs_brakes: 'Freios ABS',
  ebd: 'EBD',
  esp: 'Controle de estabilidade',
  traction_control: 'Controle de tração',
  asr: 'Tração ASR',
  hill_holder: 'Assistente de rampa',
  airbag_driver: 'Airbag motorista',
  airbag_passenger: 'Airbag passageiro',
  airbag_side: 'Airbag lateral',
  airbag_knee: 'Airbag de joelho',
  airbag_curtain: 'Airbag de cortina',
  isofix: 'Isofix',
  blind_spot_monitor: 'Sensor de ponto cego',
  lane_departure: 'Aviso de saída de faixa',

  // Sensores / câmera
  front_camera: 'Câmera frontal',
  rear_camera: 'Câmera traseira',
  parking_sensor_front: 'Sensor de estacionamento dianteiro',
  parking_sensor_rear: 'Sensor de estacionamento traseiro',

  // Conforto
  cruise_control: 'Piloto automático',
  adaptive_cruise: 'Piloto automático adaptativo',
  speed_limiter: 'Limitador de velocidade',
  rain_sensor: 'Sensor de chuva',
  headlight_auto: 'Faróis com regulagem automática',
  fog_lights_front: 'Faróis de neblina dianteiros',
  fog_lights_rear: 'Faróis de neblina traseiros',
  led_headlights: 'Faróis de LED',
  xenon_headlights: 'Faróis de xenon',
  leather_seats: 'Bancos em couro',
  heated_seats: 'Bancos aquecidos',
  electric_seats: 'Bancos elétricos',
  sunroof: 'Teto solar elétrico retrátil',
  panoramic_roof: 'Teto panorâmico',
  tinted_windows: 'Vidros escurecidos',
  ac: 'Ar-condicionado',
  digital_ac: 'Ar-condicionado digital',
  climate_control: 'Climatizador',
  defrost_rear: 'Desembaçador traseiro',
  rear_wiper: 'Limpador traseiro',
  roof_rack: 'Bagageiro no teto',
  spare_tire_holder: 'Suporte para estepe',
  alarm: 'Alarme',
  central_lock: 'Trava elétrica central',
  keyless_entry: 'Entrada sem chave',
  keyless_start: 'Partida sem chave',
  power_windows: 'Vidros elétricos',
  auto_windows: 'Fechamento automático dos vidros',
  electric_mirrors: 'Retrovisores elétricos',
  auto_folding_mirrors: 'Retrovisores rebatíveis',
  heated_mirrors: 'Retrovisores aquecidos',
  power_tailgate: 'Porta-malas elétrico',
  trip_computer: 'Computador de bordo',
  lights_on_warning: 'Alarme de luzes acesas',
  cup_holder: 'Porta copos',
  steering_controls: 'Controle remoto para rádio no volante',

  // Áudio / mídia
  am_fm: 'AM/FM',
  bluetooth: 'Bluetooth',
  cd_player: 'CD player',
  dvd_player: 'DVD player',
  mp3_player: 'Leitor de MP3',
  aux_input: 'Entrada auxiliar',
  usb_input: 'Entrada USB',
  android_auto: 'Android Auto',
  apple_carplay: 'Apple CarPlay',
  gps: 'GPS',
  wifi: 'Wi-Fi',

  // Abertura interna porta-malas
  internal_trunk_release: 'Abertura interna do porta-malas',
}

export const CONDITION_LABELS: Record<string, string> = {
  Novo: 'Novo',
  Usado: 'Usado',
  Seminovo: 'Seminovo',
}

export const DRIVETRAIN_LABELS: Record<string, string> = {
  Dianteira: 'Dianteira',
  Traseira: 'Traseira',
  '4x4': '4x4',
  AWD: 'AWD',
}

export const STEERING_LABELS: Record<string, string> = {
  Mecânica: 'Mecânica',
  Hidráulica: 'Hidráulica',
  Elétrica: 'Elétrica',
  'Eletro-hidráulica': 'Eletro-hidráulica',
}

export const TRANSMISSION_DETAIL_LABELS: Record<string, string> = {
  Manual: 'Manual',
  Automática: 'Automática',
  'Automática sequencial': 'Automática sequencial',
  CVT: 'CVT',
  Automatizada: 'Automatizada',
  'Automatizada de embreagem simples': 'Automatizada (embreagem simples)',
  'Automatizada de embreagem dupla': 'Automatizada (embreagem dupla)',
}

export const ARMOR_LEVEL_LABELS: Record<string, string> = {
  'III-A': 'Nível III-A',
  III: 'Nível III',
  II: 'Nível II',
}

export const BODY_TYPE_LABELS: Record<string, string> = {
  Sedan: 'Sedan',
  Hatch: 'Hatch',
  SUV: 'SUV',
  Cupê: 'Cupê',
  Perua: 'Perua',
  Pickup: 'Pickup',
  Van: 'Van',
  Furgão: 'Furgão',
  'Off-road': 'Off-road',
}

export const CYLINDER_LAYOUT_LABELS: Record<string, string> = {
  Linha: 'Linha',
  V: 'V',
  W: 'W',
  Boxer: 'Boxer',
  Radial: 'Radial',
  Plano: 'Plano',
  Outro: 'Outro',
}

export const FUEL_SUPPLY_LABELS: Record<string, string> = {
  'Injeção eletrônica': 'Injeção eletrônica',
  Carburador: 'Carburador',
  'Injeção direta': 'Injeção direta',
  Outro: 'Outro',
}

export const COLOR_OPTIONS = [
  'Preto', 'Branco', 'Prata', 'Cinza', 'Vermelho', 'Azul',
  'Verde', 'Amarelo', 'Marrom', 'Bege', 'Dourado', 'Laranja',
]

export const ENGINE_DISPLACEMENT_OPTIONS = [
  '1.0', '1.2', '1.3', '1.4', '1.5', '1.6', '1.8', '2.0',
  '2.4', '3.0', '4.0', '125cc', '160cc', '250cc', '300cc', '500cc',
  '650cc', '1000cc',
]
